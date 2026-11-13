package com.foodieexpress.service;

import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import com.foodieexpress.dto.CouponTotals;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private PaymentRecordRepository paymentRecordRepository;

    @Autowired private CouponService couponService;
    @Autowired private OrderStatusHistoryRepository statusHistoryRepository;

    @Transactional
    public Order placeOrder(Customer customer, String deliveryAddress, String couponCode) {
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));

        if (cart.getItems().isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        // Recheck availability and derive all prices from the current server-side catalog.
        // Group items by restaurant
        List<Restaurant> restaurants = cart.getItems().stream()
                .map(item -> item.getFoodItem().getRestaurant())
                .distinct()
                .toList();

        if (restaurants.size() > 1) {
            throw new RuntimeException("Cart contains items from multiple restaurants. Please order from one restaurant at a time.");
        }

        Restaurant restaurant = restaurants.get(0);
        if (restaurant == null || !restaurant.isActive()) {
            throw new RuntimeException("Restaurant is no longer available");
        }

        // Validate the live catalog before reusing any pending order as well as before creating a new one.
        for (CartItem cartItem : cart.getItems()) {
            if (cartItem.getQuantity() < 1 || cartItem.getQuantity() > 99) {
                throw new RuntimeException("Cart item quantity is invalid; update the cart and retry");
            }
            if (!cartItem.getFoodItem().isAvailable() || cartItem.getFoodItem().getRestaurant() == null
                    || !cartItem.getFoodItem().getRestaurant().isActive()) {
                throw new RuntimeException("Food item is no longer available: " + cartItem.getFoodItem().getName());
            }
        }

        var pendingOrder = orderRepository.findFirstByCustomerAndStatusOrderByOrderTimeDesc(
                customer, Order.OrderStatus.PLACED);
        if (pendingOrder.isPresent()) {
            Order existing = pendingOrder.get();
            boolean sameItems = existing.getItems().size() == cart.getItems().size()
                    && existing.getItems().stream().allMatch(ordered -> cart.getItems().stream().anyMatch(current ->
                    current.getFoodItem().getId().equals(ordered.getFoodItem().getId())
                            && current.getQuantity() == ordered.getQuantity()
                            && current.getFoodItem().getPrice().compareTo(ordered.getPriceAtOrder()) == 0));
            Bill existingBill = billRepository.findByOrder(existing).orElse(null);
            boolean sameCoupon = existingBill != null && java.util.Objects.equals(
                    existingBill.getCouponCode(), couponCode == null || couponCode.isBlank() ? null : couponCode.trim().toUpperCase());
            if (sameItems && sameCoupon && existingBill != null && existingBill.getPaymentStatus() == Bill.PaymentStatus.PENDING) {
                if (couponCode != null && !couponCode.isBlank()) couponService.reserve(couponCode, customer, existing, existingBill.getTotalAmount());
                existing.setDeliveryAddress(deliveryAddress);
                existing.setRestaurant(restaurant);
                return orderRepository.save(existing);
            }
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setRestaurant(restaurant);
        order.setDeliveryAddress(deliveryAddress);
        order.setStatus(Order.OrderStatus.PLACED);
        order.setOrderTime(LocalDateTime.now());

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            if (!cartItem.getFoodItem().isAvailable() || cartItem.getFoodItem().getRestaurant() == null
                    || !cartItem.getFoodItem().getRestaurant().isActive()) {
                throw new RuntimeException("Food item is no longer available: " + cartItem.getFoodItem().getName());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setFoodItem(cartItem.getFoodItem());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPriceAtOrder(cartItem.getFoodItem().getPrice());
            orderItem.setItemNameAtOrder(cartItem.getFoodItem().getName());
            orderItems.add(orderItem);
            total = total.add(cartItem.getFoodItem().getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }

        order.setItems(orderItems);
        order.setTotalAmount(total);
        order = orderRepository.save(order);
        recordStatus(order, Order.OrderStatus.PLACED, "CUSTOMER", "Order placed");

        orderItemRepository.saveAll(orderItems);

        // Generate bill
        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setTotalAmount(total);
        bill.setTax(total.multiply(new BigDecimal("0.10")).setScale(2, java.math.RoundingMode.HALF_UP));
        bill.setDeliveryFee(new BigDecimal("40.00"));
        if (couponCode != null && !couponCode.isBlank()) {
            CouponTotals pricing = couponService.preview(customer, couponCode);
            // Preview reads current cart/catalog values; order subtotal is independently calculated above.
            if (pricing.getSubtotal().compareTo(total) != 0) throw new RuntimeException("Cart prices changed. Refresh your cart and retry.");
            bill.setDiscount(pricing.getDiscount());
            bill.setDeliveryFee(pricing.getDeliveryFee());
            bill.setCouponCode(pricing.getCode());
            couponService.reserve(couponCode, customer, order, total);
        } else {
            bill.setDiscount(BigDecimal.ZERO);
        }
        bill.setGrandTotal(total.add(bill.getTax()).add(bill.getDeliveryFee()).subtract(bill.getDiscount()).max(BigDecimal.ZERO));
        bill.setPaymentStatus(Bill.PaymentStatus.PENDING);
        billRepository.save(bill);

        return order;
    }

    @Transactional
    public void clearOrderedItemsFromCart(Customer customer, Order order) {
        Cart cart = cartRepository.findByCustomer(customer)
                .orElseThrow(() -> new RuntimeException("Cart not found"));
        for (OrderItem orderedItem : order.getItems()) {
            cart.getItems().stream()
                    .filter(cartItem -> cartItem.getFoodItem().getId().equals(orderedItem.getFoodItem().getId()))
                    .findFirst()
                    .ifPresent(cartItem -> {
                        if (cartItem.getQuantity() <= orderedItem.getQuantity()) {
                            cart.removeItem(cartItem);
                            cartItemRepository.delete(cartItem);
                        } else {
                            cartItem.setQuantity(cartItem.getQuantity() - orderedItem.getQuantity());
                            cartItemRepository.save(cartItem);
                        }
                    });
        }
        cartRepository.save(cart);
    }

    public List<Order> getCustomerOrders(Customer customer) {
        return orderRepository.findByCustomerOrderByOrderTimeDesc(customer, org.springframework.data.domain.PageRequest.of(0, 100)).getContent();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getCustomerOrders(Customer customer, int page, int size) {
        return orderRepository.findByCustomerOrderByOrderTimeDesc(customer,
                org.springframework.data.domain.PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getCustomerOrders(Customer customer, int page, int size, Order.OrderStatus status) {
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return status == null ? orderRepository.findByCustomerOrderByOrderTimeDesc(customer, pageable)
                : orderRepository.findByCustomerAndStatusOrderByOrderTimeDesc(customer, status, pageable);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Order> getCustomerOrders(Customer customer, int page, int size, String status) {
        if (status == null || status.isBlank()) return getCustomerOrders(customer, page, size);
        var pageable = org.springframework.data.domain.PageRequest.of(page, size);
        if ("ACTIVE".equalsIgnoreCase(status)) return orderRepository.findByCustomerAndStatusInOrderByOrderTimeDesc(customer,
                List.of(Order.OrderStatus.PLACED, Order.OrderStatus.CONFIRMED, Order.OrderStatus.PREPARING, Order.OrderStatus.OUT_FOR_DELIVERY), pageable);
        try { return orderRepository.findByCustomerAndStatusOrderByOrderTimeDesc(customer, Order.OrderStatus.valueOf(status.toUpperCase()), pageable); }
        catch (IllegalArgumentException e) { throw new IllegalArgumentException("Invalid order status filter"); }
    }

    @Transactional(readOnly = true)
    public com.foodieexpress.dto.OrderTrackingResponse getTracking(Order order) {
        List<com.foodieexpress.dto.OrderTrackingResponse.Step> timeline = statusHistoryRepository
                .findByOrderIdOrderByChangedAtAscIdAsc(order.getId()).stream()
                .map(event -> new com.foodieexpress.dto.OrderTrackingResponse.Step(event.getStatus(), event.getChangedAt(), event.getNote()))
                .toList();
        if (timeline.isEmpty()) timeline = List.of(new com.foodieexpress.dto.OrderTrackingResponse.Step(
                Order.OrderStatus.PLACED, order.getOrderTime(), "Order placed"));
        PaymentRecord payment = paymentRecordRepository.findByOrder(order).orElse(null);
        String reference = payment != null && payment.getStatus() == PaymentRecord.PaymentStatus.SUCCESS
                ? payment.getRazorpayPaymentId() : null;
        String method = reference == null ? null : "Razorpay";
        String paymentStatus = billRepository.findByOrder(order).map(bill -> bill.getPaymentStatus().name()).orElse("PENDING");
        return new com.foodieexpress.dto.OrderTrackingResponse(order.getId(), order.getStatus(),
                timeline.get(timeline.size() - 1).changedAt(), order.getCancellationReason(), paymentStatus, method, reference, timeline);
    }

    private void recordStatus(Order order, Order.OrderStatus status, String actor, String note) {
        statusHistoryRepository.save(com.foodieexpress.model.OrderStatusHistory.builder()
                .order(order).status(status).actorRole(actor).note(note).build());
    }

    @Transactional
    public void recordPaymentConfirmation(Order order) {
        recordStatus(order, Order.OrderStatus.CONFIRMED, "PAYMENT", "Payment verified");
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getRestaurantOrders(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new RuntimeException("Restaurant not found"));
        return orderRepository.findByRestaurant(restaurant);
    }

    @Transactional
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        if (order.getStatus() == status) return order;
        boolean allowed = switch (order.getStatus()) {
            case PLACED -> status == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> status == Order.OrderStatus.PREPARING;
            case PREPARING -> status == Order.OrderStatus.OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> status == Order.OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
        if (!allowed) throw new RuntimeException("Invalid order status transition: " + order.getStatus() + " to " + status);
        if (status == Order.OrderStatus.CANCELLED) {
            PaymentRecord payment = paymentRecordRepository.findByOrder(order).orElse(null);
            if (payment != null && payment.getStatus() != PaymentRecord.PaymentStatus.FAILED
                    && payment.getStatus() != PaymentRecord.PaymentStatus.CANCELLED) {
                throw new RuntimeException("An order with an active or successful payment attempt cannot be cancelled here");
            }
            order.setCancellationReason("Cancelled by administrator");
        }
        order.setStatus(status);
        if (status == Order.OrderStatus.CANCELLED) couponService.release(order);
        Order saved = orderRepository.save(order);
        recordStatus(saved, status, "ADMIN", "Order status updated");
        return saved;
    }

    @Transactional
    public Order cancelUnpaidOrder(Long orderId) {
        return cancelUnpaidOrder(orderId, null);
    }

    @Transactional
    public Order cancelUnpaidOrder(Long orderId, String reason) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != Order.OrderStatus.PLACED) {
            throw new RuntimeException("Only unpaid orders can be cancelled");
        }
        PaymentRecord payment = paymentRecordRepository.findByOrder(order).orElse(null);
        if (payment != null && payment.getStatus() != PaymentRecord.PaymentStatus.FAILED
                && payment.getStatus() != PaymentRecord.PaymentStatus.CANCELLED) {
            throw new RuntimeException("An order with an active or successful payment attempt cannot be cancelled");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        order.setCancellationReason(reason == null || reason.isBlank() ? "Cancelled by customer" : reason.trim());
        couponService.release(order);
        Order saved = orderRepository.save(order);
        recordStatus(saved, Order.OrderStatus.CANCELLED, "CUSTOMER", saved.getCancellationReason());
        return saved;
    }
}
