package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.dto.OrderRequest;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CustomerService;
import com.foodieexpress.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@SecurityRequirement(name = "X-Session-Token")
@Tag(name = "Orders", description = "Place orders and view order history")
public class OrderController {

    @Autowired private OrderService orderService;
    @Autowired private AuthService authService;
    @Autowired private CustomerService customerService;
    @Autowired private com.foodieexpress.service.CartService cartService;

    private Customer requireCustomer(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
        if (user.getRole() != User.Role.CUSTOMER) throw new RuntimeException("Customer access required");
        return customerService.getOrCreateProfile(user);
    }

    private void assertOrderOwnership(Customer customer, Order order) {
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You can only access your own orders");
        }
    }

    @PostMapping("/place")
    @Operation(summary = "Place an order from the current cart")
    public ResponseEntity<ApiResponse<Order>> placeOrder(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody OrderRequest req) {
        Customer customer = requireCustomer(token);
        Order order = orderService.placeOrder(customer, req.getDeliveryAddress() != null
                ? req.getDeliveryAddress() : customer.getAddress(), req.getCouponCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Order placed successfully", order));
    }

    @GetMapping
    @Operation(summary = "List my orders")
    public ResponseEntity<ApiResponse<List<Order>>> myOrders(@RequestHeader("X-Session-Token") String token) {
        Customer customer = requireCustomer(token);
        List<Order> orders = orderService.getCustomerOrders(customer);
        return ResponseEntity.ok(ApiResponse.success("Orders retrieved", orders));
    }

    @GetMapping("/history")
    @Operation(summary = "Get a page of my order history")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<Order>>> orderHistory(
            @RequestHeader("X-Session-Token") String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String status) {
        if (page < 0 || size < 1 || size > 50) throw new IllegalArgumentException("Invalid page or page size");
        Customer customer = requireCustomer(token);
        return ResponseEntity.ok(ApiResponse.success("Order history", orderService.getCustomerOrders(customer, page, size, status)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order details (own orders only)")
    public ResponseEntity<ApiResponse<Order>> getOrder(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long orderId) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(orderId);
        assertOrderOwnership(customer, order);
        return ResponseEntity.ok(ApiResponse.success("Order details", order));
    }

    @GetMapping("/{orderId}/tracking")
    @Operation(summary = "Get tracking history for my order")
    public ResponseEntity<ApiResponse<com.foodieexpress.dto.OrderTrackingResponse>> getTracking(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long orderId) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(orderId);
        assertOrderOwnership(customer, order);
        return ResponseEntity.ok(ApiResponse.success("Order tracking", orderService.getTracking(order)));
    }

    @PostMapping("/{orderId}/reorder")
    @Operation(summary = "Add available items from my previous order to my cart")
    public ResponseEntity<ApiResponse<com.foodieexpress.dto.ReorderResponse>> reorder(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long orderId) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(orderId);
        assertOrderOwnership(customer, order);
        return ResponseEntity.ok(ApiResponse.success("Available items added to cart",
                cartService.addOrderToCart(customer, order)));
    }

    @PatchMapping("/{orderId}/cancel")
    @Operation(summary = "Cancel my order")
    public ResponseEntity<ApiResponse<Order>> cancelOrder(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long orderId,
            @RequestBody(required = false) java.util.Map<String, String> body) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(orderId);
        assertOrderOwnership(customer, order);
        String reason = body == null ? null : body.get("reason");
        if (reason != null && reason.length() > 500) throw new IllegalArgumentException("Cancellation reason must be 500 characters or fewer");
        Order cancelled = orderService.cancelUnpaidOrder(orderId, reason);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", cancelled));
    }
}
