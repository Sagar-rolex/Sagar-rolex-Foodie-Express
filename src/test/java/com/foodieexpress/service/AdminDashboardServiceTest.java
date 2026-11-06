package com.foodieexpress.service;

import com.foodieexpress.repository.CustomerRepository;
import com.foodieexpress.repository.OrderRepository;
import com.foodieexpress.repository.RestaurantRepository;
import com.foodieexpress.repository.FoodItemRepository;
import com.foodieexpress.repository.BillRepository;
import com.foodieexpress.repository.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminDashboardServiceTest {
    private RestaurantRepository restaurantRepository;
    private OrderRepository orderRepository;
    private CustomerRepository customerRepository;
    private FoodItemRepository foodItemRepository;
    private BillRepository billRepository;
    private OrderItemRepository orderItemRepository;
    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        restaurantRepository = mock(RestaurantRepository.class);
        orderRepository = mock(OrderRepository.class);
        customerRepository = mock(CustomerRepository.class);
        foodItemRepository = mock(FoodItemRepository.class);
        billRepository = mock(BillRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        service = new AdminDashboardService(restaurantRepository, orderRepository, customerRepository, foodItemRepository, billRepository, orderItemRepository);
    }

    @Test
    void returnsZeroStatisticsForEmptyDatabase() {
        when(restaurantRepository.count()).thenReturn(0L);
        when(orderRepository.count()).thenReturn(0L);
        when(billRepository.sumPaidRevenue(com.foodieexpress.model.Bill.PaymentStatus.PAID)).thenReturn(null);
        when(customerRepository.count()).thenReturn(0L);
        when(foodItemRepository.countByAvailableTrue()).thenReturn(0L);
        when(orderRepository.countByStatus(com.foodieexpress.model.Order.OrderStatus.PLACED)).thenReturn(0L);
        when(orderRepository.findTop5ByOrderByOrderTimeDesc()).thenReturn(List.of());
        when(orderRepository.countOrdersByDaySince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(billRepository.paidRevenueByDaySince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(orderItemRepository.findBestSellersByPaidQuantity()).thenReturn(List.of());

        var result = service.getDashboard();

        assertThat(result.totalRestaurants()).isZero();
        assertThat(result.totalOrders()).isZero();
        assertThat(result.totalRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalCustomers()).isZero();
        assertThat(result.availableFoodItems()).isZero();
        assertThat(result.pendingOrders()).isZero();
        assertThat(result.recentOrders()).isEmpty();
        assertThat(result.unavailableMetrics()).isEmpty();
    }

    @Test
    void preservesAvailableStatisticsWhenOneMetricFails() {
        when(restaurantRepository.count()).thenThrow(new RuntimeException("database query failed"));
        when(orderRepository.count()).thenReturn(8L);
        when(billRepository.sumPaidRevenue(com.foodieexpress.model.Bill.PaymentStatus.PAID)).thenReturn(new BigDecimal("125.50"));
        when(customerRepository.count()).thenReturn(3L);
        when(foodItemRepository.countByAvailableTrue()).thenReturn(4L);
        when(orderRepository.countByStatus(com.foodieexpress.model.Order.OrderStatus.PLACED)).thenReturn(2L);
        when(orderRepository.findTop5ByOrderByOrderTimeDesc()).thenReturn(List.of());
        when(orderRepository.countOrdersByDaySince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(billRepository.paidRevenueByDaySince(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(orderItemRepository.findBestSellersByPaidQuantity()).thenReturn(List.of());

        var result = service.getDashboard();

        assertThat(result.totalRestaurants()).isNull();
        assertThat(result.totalOrders()).isEqualTo(8L);
        assertThat(result.totalRevenue()).isEqualByComparingTo("125.50");
        assertThat(result.totalCustomers()).isEqualTo(3L);
        assertThat(result.availableFoodItems()).isEqualTo(4L);
        assertThat(result.pendingOrders()).isEqualTo(2L);
        assertThat(result.unavailableMetrics()).containsExactly("restaurants");
    }
}
