package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.PaymentRecord;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CustomerService;
import com.foodieexpress.service.OrderService;
import com.foodieexpress.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@SecurityRequirement(name = "X-Session-Token")
@Tag(name = "Payments", description = "Razorpay payment integration endpoints")
public class PaymentController {

    @Autowired private PaymentService paymentService;
    @Autowired private OrderService orderService;
    @Autowired private AuthService authService;
    @Autowired private CustomerService customerService;

    private Customer requireCustomer(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
        return customerService.getOrCreateProfile(user);
    }

    @PostMapping("/create-order")
    @Operation(summary = "Create Razorpay payment order for a local order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createPaymentOrder(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody CreatePaymentRequest req) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(req.getOrderId());

        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You can only pay for your own orders");
        }
        if (order.getStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Cancelled orders cannot be paid");
        }

        PaymentRecord paymentRecord = paymentService.createRazorpayOrder(customer, order);

        Map<String, Object> responseData = Map.of(
                "keyId", paymentService.getKeyId(),
                "razorpayOrderId", paymentRecord.getRazorpayOrderId(),
                "amount", paymentRecord.getAmount().movePointRight(2).longValueExact(),
                "currency", paymentRecord.getCurrency(),
                "orderId", order.getId()
        );

        return ResponseEntity.ok(ApiResponse.success("Razorpay payment order created", responseData));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment signature and mark order as paid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody VerifyPaymentRequest req) {
        Customer customer = requireCustomer(token);

        PaymentService.VerificationResult verification = paymentService.verifyPaymentSignature(
                req.getRazorpayOrderId(),
                req.getRazorpayPaymentId(),
                req.getRazorpaySignature(),
                customer
        );

        if (verification.verified()) {
            return ResponseEntity.ok(ApiResponse.success(verification.message(), Map.of("status", verification.status())));
        }
        return ResponseEntity.badRequest().body(ApiResponse.error(verification.message()));
    }

    @PostMapping("/failed")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> failedPayment(
            @RequestHeader("X-Session-Token") String token, @RequestBody Map<String,String> req) {
        Customer customer=requireCustomer(token);
        String paymentId=req.get("razorpayPaymentId");
        if(paymentId==null||paymentId.isBlank()) return ResponseEntity.badRequest().body(ApiResponse.error("Razorpay payment ID is required"));
        boolean failed=paymentService.recordFailedPayment(customer,paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment status checked",Map.of("failed",failed)));
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CreatePaymentRequest {
        @NotNull(message = "Order ID is required")
        private Long orderId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class VerifyPaymentRequest {
        @NotBlank(message = "Razorpay order ID is required")
        private String razorpayOrderId;
        @NotBlank(message = "Razorpay payment ID is required")
        private String razorpayPaymentId;
        @NotBlank(message = "Razorpay signature is required")
        private String razorpaySignature;
    }
}
