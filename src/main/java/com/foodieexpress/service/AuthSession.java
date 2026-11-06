package com.foodieexpress.service;

import com.foodieexpress.model.User;

public record AuthSession(User user, String token) {
}
