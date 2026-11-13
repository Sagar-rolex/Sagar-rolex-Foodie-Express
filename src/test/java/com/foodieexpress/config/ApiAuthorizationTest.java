package com.foodieexpress.config;

import com.foodieexpress.controller.AdminDashboardController;
import com.foodieexpress.controller.AuthController;
import com.foodieexpress.controller.RestaurantController;
import com.foodieexpress.dto.AdminDashboardResponse;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AdminAuthorization;
import com.foodieexpress.service.AdminDashboardService;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CustomerService;
import com.foodieexpress.service.FoodImageStorageService;
import com.foodieexpress.service.FoodItemService;
import com.foodieexpress.service.OrderService;
import com.foodieexpress.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RestaurantController.class, AdminDashboardController.class, AuthController.class})
@Import({SecurityConfig.class, ApiAuthorizationTest.UploadTestConfig.class})
class ApiAuthorizationTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;
    @MockBean private CustomerService customerService;
    @MockBean private AdminAuthorization adminAuthorization;
    @MockBean private RestaurantService restaurantService;
    @MockBean private FoodItemService foodItemService;
    @MockBean private OrderService orderService;
    @MockBean private AdminDashboardService adminDashboardService;

    @TestConfiguration
    static class UploadTestConfig {
        @Bean
        FoodImageStorageService foodImageStorageService() {
            return new FoodImageStorageService("target/test-uploads");
        }
    }

    @BeforeEach
    void stubPublicData() {
        when(restaurantService.getAllActive()).thenReturn(List.of());
        when(adminDashboardService.getDashboard(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new AdminDashboardResponse(
                        0L, 0L, java.math.BigDecimal.ZERO, 0L, 0L, 0L, List.of(), List.of(), List.of(), List.of(), List.of(),
                        0L, 0L, 0L, 0L, 0L, java.math.BigDecimal.ZERO,
                        new AdminDashboardResponse.RevenueSummary(java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO),
                        List.of(), List.of(), "2026-01-01", "2026-01-30", "daily"));
    }

    @Test
    void restaurantCatalogIsPublic() throws Exception {
        mockMvc.perform(get("/api/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void cartRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Valid session required"));
    }

    @Test
    void adminDashboardRejectsAnonymous() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminDashboardRejectsCustomerSession() throws Exception {
        User customer = User.builder().id(1L).username("customer").role(User.Role.CUSTOMER).build();
        when(authService.validateSession("customer-token")).thenReturn(Optional.of(customer));

        mockMvc.perform(get("/api/admin/dashboard").header("X-Session-Token", "customer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminDashboardAllowsAdminSession() throws Exception {
        User admin = User.builder().id(2L).username("admin").role(User.Role.ADMIN).build();
        when(authService.validateSession("admin-token")).thenReturn(Optional.of(admin));

        mockMvc.perform(get("/api/admin/dashboard").header("X-Session-Token", "admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void loginRemainsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"customer@foodie.com\",\"password\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
