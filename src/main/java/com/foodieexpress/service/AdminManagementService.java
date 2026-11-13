package com.foodieexpress.service;

import com.foodieexpress.dto.AdminCustomerResponse;
import com.foodieexpress.dto.AdminOrderResponse;
import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class AdminManagementService {
    private static final Logger log = LoggerFactory.getLogger(AdminManagementService.class);
    private final FoodItemRepository foodItems;
    private final RestaurantRepository restaurants;
    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final BillRepository bills;
    private final PaymentRecordRepository payments;

    public AdminManagementService(FoodItemRepository foodItems, RestaurantRepository restaurants, OrderRepository orders,
            CustomerRepository customers, BillRepository bills, PaymentRecordRepository payments) {
        this.foodItems = foodItems; this.restaurants = restaurants; this.orders = orders;
        this.customers = customers; this.bills = bills; this.payments = payments;
    }

    public Page<FoodItem> listFood(String search, Long restaurantId, Boolean available, Pageable pageable) {
        return foodItems.searchForAdmin(blankToNull(search), restaurantId, available, pageable);
    }

    public Page<AdminCustomerResponse> listCustomers(String search, Boolean active, Pageable pageable) {
        return customers.searchForAdmin(blankToNull(search), active, User.Role.CUSTOMER, pageable).map(customer ->
                new AdminCustomerResponse(customer.getId(), customer.getFullName(), customer.getUser().getUsername(),
                        customer.getUser().getEmail(), customer.getPhone(), customer.getUser().getCreatedAt(),
                        !Boolean.FALSE.equals(customer.getUser().getActive()), orders.countByCustomer(customer)));
    }

    @Transactional
    public AdminCustomerResponse setCustomerActive(Long customerId, boolean active, User actor) {
        Customer customer = customers.findById(customerId).orElseThrow(() -> new RuntimeException("Customer not found"));
        User target = customer.getUser();
        if (target.getRole() != User.Role.CUSTOMER) throw new RuntimeException("Only customer accounts can be changed here");
        boolean wasActive = !Boolean.FALSE.equals(target.getActive());
        if (wasActive == active) return new AdminCustomerResponse(customer.getId(), customer.getFullName(), target.getUsername(), target.getEmail(), customer.getPhone(), target.getCreatedAt(), active, orders.countByCustomer(customer));
        target.setActive(active);
        if (!active) target.setSessionToken(null);
        log.info("Admin {} changed customer account {} active={}", actor.getUsername(), target.getId(), active);
        return new AdminCustomerResponse(customer.getId(), customer.getFullName(), target.getUsername(), target.getEmail(), customer.getPhone(), target.getCreatedAt(), active, orders.countByCustomer(customer));
    }

    @Transactional(readOnly = true)
    public Page<AdminOrderResponse> listOrders(String search, Order.OrderStatus status, Bill.PaymentStatus paymentStatus,
            Long restaurantId, LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTimeRange range = dateRange(from, to);
        Long orderId = null;
        String normalized = blankToNull(search);
        if (normalized != null && normalized.matches("\\d+")) {
            try { orderId = Long.valueOf(normalized); } catch (NumberFormatException ignored) { }
        }
        return orders.searchForAdmin(normalized, status, paymentStatus, restaurantId, range.start, range.end, orderId, pageable).map(this::toAdminOrder);
    }

    private AdminOrderResponse toAdminOrder(Order order) {
        Bill bill = bills.findByOrder(order).orElse(null);
        PaymentRecord payment = payments.findByOrder(order).orElse(null);
        String reference = payment != null && payment.getStatus() == PaymentRecord.PaymentStatus.SUCCESS
                ? payment.getRazorpayPaymentId() : null;
        var user = order.getCustomer().getUser();
        return new AdminOrderResponse(order.getId(), order.getCustomer().getFullName(), user.getEmail(),
                order.getCustomer().getPhone(), order.getRestaurant().getName(), order.getOrderTime(), order.getTotalAmount(),
                bill == null ? order.getTotalAmount() : bill.getGrandTotal(), "INR", order.getStatus(),
                bill == null ? Bill.PaymentStatus.PENDING : bill.getPaymentStatus(),
                payment != null && payment.getStatus() == PaymentRecord.PaymentStatus.SUCCESS ? "Razorpay" : null,
                reference, order.getDeliveryAddress(), order.getCancellationReason(), order.getItems().stream().map(item -> new AdminOrderResponse.Item(
                        item.getItemNameAtOrder() == null || item.getItemNameAtOrder().isBlank() ? item.getFoodItem().getName() : item.getItemNameAtOrder(),
                        item.getQuantity(), item.getPriceAtOrder())).toList());
    }

    public java.util.List<Restaurant> listRestaurants() { return restaurants.findAll(); }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private LocalDateTimeRange dateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null && to.isBefore(from)) throw new RuntimeException("End date must be on or after start date");
        return new LocalDateTimeRange(from == null ? null : from.atStartOfDay(), to == null ? null : to.plusDays(1).atStartOfDay());
    }
    private record LocalDateTimeRange(java.time.LocalDateTime start, java.time.LocalDateTime end) {}
}
