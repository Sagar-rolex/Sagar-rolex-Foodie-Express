package com.foodieexpress.dto;

import com.foodieexpress.model.Order;

import java.time.LocalDateTime;
import java.util.List;

public record OrderTrackingResponse(
        Long orderId,
        Order.OrderStatus currentStatus,
        LocalDateTime latestUpdatedAt,
        String cancellationReason,
        String paymentStatus,
        String paymentMethod,
        String paymentReference,
        List<Step> timeline
) {
    public record Step(Order.OrderStatus status, LocalDateTime changedAt, String note) {}
}
