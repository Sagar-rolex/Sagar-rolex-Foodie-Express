package com.foodieexpress.dto;

import com.foodieexpress.model.Order;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        Long totalRestaurants,
        Long totalOrders,
        BigDecimal totalRevenue,
        Long totalCustomers,
        Long availableFoodItems,
        Long pendingOrders,
        List<RecentOrder> recentOrders,
        List<TrendPoint> ordersOverTime,
        List<TrendPoint> revenueOverTime,
        List<BestSeller> bestSellingDishes,
        List<String> unavailableMetrics
) {
    public record RecentOrder(Long id, String restaurantName, BigDecimal totalAmount, Order.OrderStatus status) {}
    public record TrendPoint(String date, BigDecimal value) {}
    public record BestSeller(String name, Long quantity) {}
}
