package com.foodieexpress.dto;

import com.foodieexpress.model.Order;
import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull Order.OrderStatus status) {}
