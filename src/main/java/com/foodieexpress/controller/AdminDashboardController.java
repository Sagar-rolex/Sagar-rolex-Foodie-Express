package com.foodieexpress.controller;

import com.foodieexpress.dto.AdminDashboardResponse;
import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AdminDashboardService;
import com.foodieexpress.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {
    private final AdminDashboardService dashboardService;
    private final AuthService authService;

    public AdminDashboardController(AdminDashboardService dashboardService, AuthService authService) {
        this.dashboardService = dashboardService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getDashboard(
            @RequestHeader(value = "X-Session-Token", required = false) String token) {
        User user = token == null || token.isBlank()
                ? null
                : authService.validateSession(token).orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.<AdminDashboardResponse>error("Valid admin session required"));
        }
        if (user.getRole() != User.Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.<AdminDashboardResponse>error("Admin access required"));
        }

        return ResponseEntity.ok(ApiResponse.success("Admin dashboard", dashboardService.getDashboard()));
    }
}
