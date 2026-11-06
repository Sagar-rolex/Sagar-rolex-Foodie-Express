package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.dto.LoginRequest;
import com.foodieexpress.dto.LoginResponse;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthSession;
import com.foodieexpress.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@Tag(name = "Admin Authentication", description = "Admin login and logout")
public class AdminAuthController {

    @Autowired
    private AuthService authService;

    @PostMapping({"/api/admin/login", "/api/auth/admin/login"})
    @Operation(summary = "Admin login", description = "Authenticates admin and returns a JWT bearer token")
    public ResponseEntity<ApiResponse<LoginResponse>> adminLogin(@Valid @RequestBody LoginRequest req) {
        String identifier = req.getIdentifier();
        if (identifier.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error("Please enter your admin email address"));
        }
        Optional<AuthSession> sessionOpt = authService.login(identifier, req.getPassword());
        if (sessionOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("The email or password you entered is incorrect. Please check your details and try again."));
        }

        AuthSession session = sessionOpt.get();
        User user = session.user();
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied. Admin account required."));
        }

        return ResponseEntity.ok(ApiResponse.success("Admin login successful",
                LoginResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .role("ADMIN")
                        .sessionToken(session.token())
                        .message("Admin login successful")
                        .build()
        ));
    }

    @PostMapping({"/api/admin/logout", "/api/auth/admin/logout"})
    @Operation(summary = "Admin logout", description = "Invalidates the admin session token")
    public ResponseEntity<ApiResponse<String>> adminLogout(@RequestHeader(value = "X-Session-Token", required = false) String token) {
         if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing session token"));
        }
        authService.logout(token);
        return ResponseEntity.ok(ApiResponse.success("Admin logged out successfully", null));
    }
}