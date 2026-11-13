package com.foodieexpress.dto;

import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderResponse(
        Long id,
        String customerName,
        String customerEmail,
        String customerPhone,
        String restaurantName,
        LocalDateTime orderTime,
        BigDecimal subtotal,
        BigDecimal totalAmount,
        String currency,
        Order.OrderStatus status,
        Bill.PaymentStatus paymentStatus,
        String paymentMethod,
        String paymentReference,
        String deliveryAddress,
        String cancellationReason,
        List<Item> items
) {
    public record Item(String name, int quantity, BigDecimal unitPrice) {}
}
