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
        return orderRepository.findByCustomer(customer);
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
        if (status == Order.OrderStatus.CANCELLED && paymentRecordRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("An order with a payment attempt cannot be cancelled here; verify payment/refund status first");
        }
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelUnpaidOrder(Long orderId) {
        Order order = getOrderById(orderId);
        if (order.getStatus() != Order.OrderStatus.PLACED) {
            throw new RuntimeException("Only unpaid orders can be cancelled");
        }
        if (paymentRecordRepository.findByOrder(order).isPresent()) {
            throw new RuntimeException("An order with an active payment attempt cannot be cancelled");
        }
        order.setStatus(Order.OrderStatus.CANCELLED);
        couponService.release(order);
        return orderRepository.save(order);
    }
}
