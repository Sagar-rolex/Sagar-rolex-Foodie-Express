package com.foodieexpress.service;

import com.foodieexpress.dto.AdminDashboardResponse;
import com.foodieexpress.model.Order;
import com.foodieexpress.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Service
public class AdminDashboardService {
    private static final Logger log = LoggerFactory.getLogger(AdminDashboardService.class);

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

    public AdminDashboardResponse getDashboard() {
        List<String> unavailable = new ArrayList<>();
        Long restaurants = load("restaurants", restaurantRepository::count, unavailable);
        Long orders = load("orders", orderRepository::count, unavailable);
        BigDecimal revenue = load("revenue", () -> billRepository.sumPaidRevenue(com.foodieexpress.model.Bill.PaymentStatus.PAID), unavailable);
        Long customers = load("customers", customerRepository::count, unavailable);
        Long foods = load("availableFoodItems", foodItemRepository::countByAvailableTrue, unavailable);
        Long pending = load("pendingOrders", () -> orderRepository.countByStatus(Order.OrderStatus.PLACED), unavailable);
        List<AdminDashboardResponse.RecentOrder> recent = load("recentOrders", () ->
                orderRepository.findTop5ByOrderByOrderTimeDesc().stream().map(this::toRecentOrder).toList(), unavailable);

        LocalDateTime since = LocalDate.now().minusDays(6).atStartOfDay();
        List<AdminDashboardResponse.TrendPoint> orderTrend = load("ordersOverTime", () -> orderRepository.countOrdersByDaySince(since)
                .stream().map(point -> new AdminDashboardResponse.TrendPoint(point.getDay().toLocalDate().toString(), BigDecimal.valueOf(point.getTotal()))).toList(), unavailable);
        List<AdminDashboardResponse.TrendPoint> revenueTrend = load("revenueOverTime", () -> billRepository.paidRevenueByDaySince(since)
                .stream().map(point -> new AdminDashboardResponse.TrendPoint(point.getDay().toLocalDate().toString(), point.getTotal())).toList(), unavailable);
        List<AdminDashboardResponse.BestSeller> bestSellers = load("bestSellingDishes", () -> orderItemRepository.findBestSellersByPaidQuantity()
                .stream().map(item -> new AdminDashboardResponse.BestSeller(item.getName(), item.getQuantity())).toList(), unavailable);

        return new AdminDashboardResponse(restaurants, orders, revenue == null && unavailable.contains("revenue") ? null : revenue == null ? BigDecimal.ZERO : revenue,
                customers, foods, pending, recent == null ? List.of() : recent,
                orderTrend == null ? List.of() : orderTrend, revenueTrend == null ? List.of() : revenueTrend,
                bestSellers == null ? List.of() : bestSellers, List.copyOf(unavailable));
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
