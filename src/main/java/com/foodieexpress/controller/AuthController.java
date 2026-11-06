package com.foodieexpress.controller;

import com.foodieexpress.dto.*;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthSession;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Register, login, and session management")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private com.foodieexpress.service.AdminAuthorization adminAuthorization;

    @PostMapping("/register")
    @Operation(summary = "Register a new customer", description = "Creates a customer account and linked user credentials")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest req) {
        User user = new User();
        user.setUsername(req.getEffectiveUsername());
        user.setPassword(req.getPassword());
        user.setEmail(req.getEmail());
        User saved = authService.register(user);

        // Create a customer profile for the new user
        try {
            customerService.createProfile(saved, req.getFullName(), req.getPhone(), req.getAddress());
        } catch (Exception ignored) {}

        AuthSession session = authService.createSession(saved);
        saved = session.user();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Registration successful", new LoginResponse(saved.getId(), saved.getUsername(), saved.getRole().name(), session.token(), "Registered successfully"))
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Customer / Admin login", description = "Authenticates with username or email and password, returning a JWT bearer token")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest req) {
        String identifier = req.getIdentifier();
        if (identifier.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Please enter your email address"));
        }
        Optional<AuthSession> sessionOpt = authService.login(identifier, req.getPassword());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("The email or password you entered is incorrect. Please check your details and try again."));
        }
        AuthSession session = sessionOpt.get();
        User user = session.user();
        return ResponseEntity.ok(ApiResponse.success("Login successful",
                LoginResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role(user.getRole().name())
                        .sessionToken(session.token())
                        .message("Login successful")
                        .build()
        ));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidates the current session token")
    public ResponseEntity<ApiResponse<String>> logout(@RequestHeader(value = "X-Session-Token", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing session token"));
        }
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/admin/register")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register an admin (admin session required)", description = "Creates an additional admin account")
    public ResponseEntity<ApiResponse<LoginResponse>> adminRegister(@RequestHeader(value = "X-Session-Token", required = false) String token,
            @Valid @RequestBody RegisterRequest req) {
        adminAuthorization.requireAdmin(token);
        User user = new User();
        user.setUsername(req.getEffectiveUsername());
        user.setPassword(req.getPassword());
        user.setEmail(req.getEmail());
        User saved = authService.adminRegister(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Admin registered successfully", new LoginResponse(saved.getId(), saved.getUsername(), saved.getRole().name(), null, "Admin registered"))
        );
    }
}
