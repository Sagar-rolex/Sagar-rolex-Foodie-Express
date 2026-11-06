package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.BillService;
import com.foodieexpress.service.CustomerService;
import com.foodieexpress.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
@SecurityRequirement(name = "X-Session-Token")
@Tag(name = "Bills", description = "View and manage bills")
public class BillController {

    @Autowired private BillService billService;
    @Autowired private OrderService orderService;
    @Autowired private AuthService authService;
    @Autowired private CustomerService customerService;

    private Customer requireCustomer(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
        return customerService.getOrCreateProfile(user);
    }

    private void assertOrderOwnership(Customer customer, Order order) {
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new RuntimeException("You can only view bills for your own orders");
        }
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get bill for an order (own orders only)")
    public ResponseEntity<ApiResponse<Bill>> getBillByOrder(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long orderId) {
        Customer customer = requireCustomer(token);
        Order order = orderService.getOrderById(orderId);
        assertOrderOwnership(customer, order);
        Bill bill = billService.getBillByOrder(orderId)
                .orElseThrow(() -> new RuntimeException("Bill not found for order " + orderId));
        return ResponseEntity.ok(ApiResponse.success("Bill retrieved", bill));
    }

    @GetMapping("/my")
    @Operation(summary = "List my bills")
    public ResponseEntity<ApiResponse<List<Bill>>> myBills(@RequestHeader("X-Session-Token") String token) {
        Customer customer = requireCustomer(token);
        List<Bill> bills = billService.getCustomerBills(customer);
        return ResponseEntity.ok(ApiResponse.success("Bills retrieved", bills));
    }

    @GetMapping("/{billId}")
    @Operation(summary = "Get bill by id (own bills only)")
    public ResponseEntity<ApiResponse<Bill>> getBill(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long billId) {
        Customer customer = requireCustomer(token);
        Bill bill = billService.getBillById(billId);
        assertOrderOwnership(customer, bill.getOrder());
        return ResponseEntity.ok(ApiResponse.success("Bill details", bill));
    }

}
