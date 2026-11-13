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
        List<String> unavailableMetrics,
        Long completedOrders,
        Long cancelledOrders,
        Long periodOrders,
        Long previousPeriodOrders,
        Long newCustomers,
        BigDecimal averageOrderValue,
        RevenueSummary revenueSummary,
        List<StatusCount> orderStatusDistribution,
        List<TrendPoint> customerGrowth,
        String fromDate,
        String toDate,
        String grouping
) {
    public record RecentOrder(Long id, String restaurantName, BigDecimal totalAmount, Order.OrderStatus status) {}
    public record TrendPoint(String date, BigDecimal value) {}
    public record BestSeller(String name, String restaurantName, String imagePath, Long quantity, BigDecimal revenue) {}
    public record StatusCount(Order.OrderStatus status, Long count) {}
    public record RevenueSummary(BigDecimal today, BigDecimal thisWeek, BigDecimal thisMonth, BigDecimal allTime) {}
}
