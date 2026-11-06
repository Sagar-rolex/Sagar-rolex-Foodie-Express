package com.foodieexpress.security;

import com.foodieexpress.model.User;

public record SessionPrincipal(Long id, String username, String email, User.Role role) {
    public boolean isAdmin() {
        return role == User.Role.ADMIN;
    }

    public boolean isCustomer() {
        return role == User.Role.CUSTOMER;
    }
}
