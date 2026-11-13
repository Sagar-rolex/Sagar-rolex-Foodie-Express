package com.foodieexpress.service;

import com.foodieexpress.dto.AdminDashboardResponse;
import com.foodieexpress.model.Bill;
import com.foodieexpress.model.Order;
import com.foodieexpress.model.User;
import com.foodieexpress.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AdminDashboardService {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);
    private static final List<Order.OrderStatus> ACTIVE_STATUSES = List.of(
            Order.OrderStatus.PLACED, Order.OrderStatus.CONFIRMED,
            Order.OrderStatus.PREPARING, Order.OrderStatus.OUT_FOR_DELIVERY);

    private final RestaurantRepository restaurantRepository;
    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final FoodItemRepository foodItemRepository;
    private final BillRepository billRepository;
    private final OrderItemRepository orderItemRepository;

    public AdminDashboardService(RestaurantRepository restaurantRepository, OrderRepository orderRepository,
            CustomerRepository customerRepository, FoodItemRepository foodItemRepository,
            BillRepository billRepository, OrderItemRepository orderItemRepository) {
        this.restaurantRepository = restaurantRepository;
        this.orderRepository = orderRepository;
        this.customerRepository = customerRepository;
        this.foodItemRepository = foodItemRepository;
        this.billRepository = billRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        return getDashboard(null, null, null);
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(LocalDate from, LocalDate to, String grouping) {
        LocalDate today = LocalDate.now();
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        if (end.isBefore(start)) throw new IllegalArgumentException("End date must be on or after start date");
        if (ChronoUnit.DAYS.between(start, end) > 366) throw new IllegalArgumentException("Choose a date range of one year or less");

        String normalizedGrouping = grouping == null ? "daily" : grouping.trim().toLowerCase();
        String format = switch (normalizedGrouping) {
            case "daily" -> "%Y-%m-%d";
            case "weekly" -> "%x-W%v";
            case "monthly" -> "%Y-%m";
            default -> throw new IllegalArgumentException("Grouping must be daily, weekly, or monthly");
        };

        LocalDateTime startAt = start.atStartOfDay();
        LocalDateTime endAt = end.plusDays(1).atStartOfDay();
        LocalDateTime previousStart = start.minusDays(ChronoUnit.DAYS.between(start, end) + 1).atStartOfDay();
        LocalDateTime previousEnd = startAt;
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate monthStart = today.withDayOfMonth(1);

        List<String> unavailable = new ArrayList<>();
        Long restaurants = load("restaurants", restaurantRepository::count, unavailable);
        Long totalOrders = load("orders", orderRepository::count, unavailable);
        Long totalCustomers = load("customers", customerRepository::count, unavailable);
        Long availableFoods = load("availableFoodItems", foodItemRepository::countByAvailableTrue, unavailable);
        BigDecimal allTimeRevenue = load("revenueAllTime", () -> billRepository.sumPaidRevenue(
                Bill.PaymentStatus.PAID, com.foodieexpress.model.PaymentRecord.PaymentStatus.SUCCESS, Order.OrderStatus.CANCELLED), unavailable);
        Long activeOrders = load("pendingOrders", () -> orderRepository.countByStatusIn(ACTIVE_STATUSES), unavailable);
        Long completedOrders = load("completedOrders", () -> orderRepository.countByStatus(Order.OrderStatus.DELIVERED), unavailable);
        Long cancelledOrders = load("cancelledOrders", () -> orderRepository.countByStatus(Order.OrderStatus.CANCELLED), unavailable);
        Long periodOrders = load("periodOrders", () -> orderRepository.countByOrderTimeGreaterThanEqualAndOrderTimeLessThan(startAt, endAt), unavailable);
        Long previousOrders = load("previousPeriodOrders", () -> orderRepository.countByOrderTimeGreaterThanEqualAndOrderTimeLessThan(previousStart, previousEnd), unavailable);
        Long newCustomers = load("newCustomers", () -> customerRepository.countCreatedBetween(startAt, endAt, User.Role.CUSTOMER), unavailable);
        BigDecimal periodRevenue = load("periodRevenue", () -> paidRevenue(startAt, endAt), unavailable);
        Long paidOrders = load("paidOrders", () -> billRepository.countPaidBetween(Bill.PaymentStatus.PAID,
                com.foodieexpress.model.PaymentRecord.PaymentStatus.SUCCESS, Order.OrderStatus.CANCELLED, startAt, endAt), unavailable);
        BigDecimal averageOrderValue = periodRevenue == null || paidOrders == null ? null
                : paidOrders == 0 ? BigDecimal.ZERO : periodRevenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP);

        BigDecimal revenueToday = load("todayRevenue", () -> paidRevenue(todayStart, todayStart.plusDays(1)), unavailable);
        BigDecimal revenueWeek = load("weekRevenue", () -> paidRevenue(weekStart.atStartOfDay(), todayStart.plusDays(1)), unavailable);
        BigDecimal revenueMonth = load("monthRevenue", () -> paidRevenue(monthStart.atStartOfDay(), todayStart.plusDays(1)), unavailable);

        List<AdminDashboardResponse.RecentOrder> recent = load("recentOrders", () ->
                orderRepository.findTop5ByOrderByOrderTimeDesc().stream().map(this::toRecentOrder).toList(), unavailable);
        List<AdminDashboardResponse.TrendPoint> orderTrend = load("orderVolume", () -> orderRepository.countOrdersByBucket(startAt, endAt, format)
                .stream().map(point -> new AdminDashboardResponse.TrendPoint(point.getBucket(), BigDecimal.valueOf(point.getTotal()))).toList(), unavailable);
        List<AdminDashboardResponse.TrendPoint> revenueTrend = load("revenueAnalytics", () -> billRepository.paidRevenueByBucket(startAt, endAt, format)
                .stream().map(point -> new AdminDashboardResponse.TrendPoint(point.getBucket(), point.getTotal())).toList(), unavailable);
        List<AdminDashboardResponse.TrendPoint> customerGrowth = load("customerGrowth", () -> customerRepository.customerGrowthByBucket(startAt, endAt, format)
                .stream().map(point -> new AdminDashboardResponse.TrendPoint(point.getBucket(), BigDecimal.valueOf(point.getTotal()))).toList(), unavailable);
        List<AdminDashboardResponse.BestSeller> bestSellers = load("bestSellingDishes", () -> orderItemRepository.findBestSellersBetween(startAt, endAt)
                .stream().map(item -> new AdminDashboardResponse.BestSeller(item.getName(), item.getRestaurantName(), item.getImagePath(), item.getQuantity(), item.getRevenue())).toList(), unavailable);
        List<AdminDashboardResponse.StatusCount> statusDistribution = load("orderStatusDistribution", () -> java.util.Arrays.stream(Order.OrderStatus.values())
                .map(status -> new AdminDashboardResponse.StatusCount(status,
                        orderRepository.countByStatusAndOrderTimeGreaterThanEqualAndOrderTimeLessThan(status, startAt, endAt))).toList(), unavailable);

        return new AdminDashboardResponse(
                restaurants, totalOrders, allTimeRevenue, totalCustomers, availableFoods, activeOrders,
                recent == null ? List.of() : recent,
                orderTrend == null ? List.of() : orderTrend,
                revenueTrend == null ? List.of() : revenueTrend,
                bestSellers == null ? List.of() : bestSellers,
                List.copyOf(unavailable),
                completedOrders, cancelledOrders, periodOrders, previousOrders, newCustomers,
                averageOrderValue,
                new AdminDashboardResponse.RevenueSummary(revenueToday, revenueWeek, revenueMonth, allTimeRevenue),
                statusDistribution == null ? List.of() : statusDistribution,
                customerGrowth == null ? List.of() : customerGrowth,
                start.toString(), end.toString(), normalizedGrouping
        );
    }

    private BigDecimal paidRevenue(LocalDateTime from, LocalDateTime to) {
        return billRepository.sumPaidRevenueBetween(Bill.PaymentStatus.PAID,
                com.foodieexpress.model.PaymentRecord.PaymentStatus.SUCCESS, Order.OrderStatus.CANCELLED, from, to);
    }

    private AdminDashboardResponse.RecentOrder toRecentOrder(Order order) {
        return new AdminDashboardResponse.RecentOrder(order.getId(), order.getRestaurant() == null ? null : order.getRestaurant().getName(), order.getTotalAmount(), order.getStatus());
    }

    private <T> T load(String metric, Supplier<T> query, List<String> unavailable) {
        try { return query.get(); }
        catch (RuntimeException exception) {
            log.error("Unable to load admin dashboard metric: {}", metric, exception);
            unavailable.add(metric);
            return null;
        }
    }
}
