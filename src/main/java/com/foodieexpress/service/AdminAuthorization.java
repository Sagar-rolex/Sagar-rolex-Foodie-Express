package com.foodieexpress.service;

import com.foodieexpress.model.User;
import org.springframework.stereotype.Component;

@Component
public class AdminAuthorization {
    private final AuthService authService;

    public AdminAuthorization(AuthService authService) {
        this.authService = authService;
    }

    public User requireAdmin(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Valid admin session required"));
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Admin access required");
        }
        return user;
    }
}
