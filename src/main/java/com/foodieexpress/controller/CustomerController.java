package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Customers", description = "Customer profile management")
public class CustomerController {

    @Autowired private AuthService authService;
    @Autowired private CustomerService customerService;

    private User requireSession(String token) {
        return authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get my profile", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<Customer>> getMyProfile(@RequestHeader("X-Session-Token") String token) {
        User user = requireSession(token);
        Customer customer = customerService.getOrCreateProfile(user);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved", customer));
    }

    @PutMapping("/me")
    @Operation(summary = "Update my profile", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<Customer>> updateMyProfile(
            @RequestHeader("X-Session-Token") String token,
            @RequestBody Map<String, String> body) {
        User user = requireSession(token);
        Customer customer = customerService.getOrCreateProfile(user);
        Customer updated = customerService.updateProfile(
                customer,
                body.get("fullName"), body.get("phone"), body.get("address"));
        return ResponseEntity.ok(ApiResponse.success("Profile updated", updated));
    }
}
