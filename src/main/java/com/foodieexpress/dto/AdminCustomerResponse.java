package com.foodieexpress.dto;

import java.time.LocalDateTime;

public record AdminCustomerResponse(Long id, String fullName, String username, String email, String phone,
                                    LocalDateTime createdAt, boolean active, long orderCount) {}
