package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.dto.FoodItemRequest;
import com.foodieexpress.dto.RestaurantRequest;
import com.foodieexpress.model.FoodItem;
import com.foodieexpress.model.Restaurant;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.FoodItemService;
import com.foodieexpress.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Restaurants & Menu", description = "Browse restaurants and menu items (public) — manage (admin)")
public class RestaurantController {

    @Autowired private RestaurantService restaurantService;
    @Autowired private FoodItemService foodItemService;
    @Autowired private AuthService authService;
    @Autowired private com.foodieexpress.service.OrderService orderService;

    private void requireAdmin(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
        if (user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("Admin access required");
        }
    }

    // ---- Public: Browse ----

    @GetMapping("/restaurants")
    @Operation(summary = "List restaurants")
    public ResponseEntity<ApiResponse<List<Restaurant>>> listRestaurants() {
        return ResponseEntity.ok(ApiResponse.success("Restaurants", restaurantService.getAllActive()));
    }

    @GetMapping("/restaurants/{id}")
    @Operation(summary = "Get restaurant by id")
    public ResponseEntity<ApiResponse<Restaurant>> getRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Restaurant details", restaurantService.getById(id)));
    }

    @GetMapping("/restaurants/{restaurantId}/items")
    @Operation(summary = "List menu items of a restaurant")
    public ResponseEntity<ApiResponse<List<FoodItem>>> getMenuByRestaurant(@PathVariable Long restaurantId) {
        return ResponseEntity.ok(ApiResponse.success("Menu items", foodItemService.getAvailableByRestaurant(restaurantId)));
    }

    @GetMapping("/food-items/search")
    @Operation(summary = "Search food items by keyword")
    public ResponseEntity<ApiResponse<List<FoodItem>>> searchFoodItems(@RequestParam("q") String query) {
        return ResponseEntity.ok(ApiResponse.success("Search results", foodItemService.searchByName(query)));
    }

    @GetMapping("/food-items/category/{category}")
    @Operation(summary = "List food items by category")
    public ResponseEntity<ApiResponse<List<FoodItem>>> getFoodItemsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.success("Items by category", foodItemService.getByCategory(category)));
    }

    @GetMapping("/food-items/{id}")
    @Operation(summary = "Get a single food item")
    public ResponseEntity<ApiResponse<FoodItem>> getFoodItem(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Food item", foodItemService.getById(id)));
    }

    // ---- Admin: Manage Restaurants ----

    @PostMapping("/admin/restaurants")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add restaurant (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<Restaurant>> addRestaurant(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody RestaurantRequest req) {
        requireAdmin(token);
        Restaurant r = restaurantService.create(req.getName(), req.getAddress(), req.getCuisineType(), req.getDescription(), req.getRating(), req.getDeliveryTime(), req.getImagePath(), req.getBannerImagePath());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Restaurant created", r));
    }

    @PutMapping("/admin/restaurants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update restaurant (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<Restaurant>> updateRestaurant(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody RestaurantRequest req) {
        requireAdmin(token);
        Restaurant r = restaurantService.update(id, req.getName(), req.getAddress(), req.getCuisineType(), req.getDescription(), req.getRating(), req.getDeliveryTime(), null, req.getImagePath(), req.getBannerImagePath());
        return ResponseEntity.ok(ApiResponse.success("Restaurant updated", r));
    }

    @PatchMapping("/admin/restaurants/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Restaurant>> setRestaurantActive(@RequestHeader("X-Session-Token") String token,
            @PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        requireAdmin(token);
        Boolean active = body.get("active");
        if (active == null) throw new RuntimeException("active must be true or false");
        Restaurant r = restaurantService.update(id, null, null, null, null, null, null, active, null, null);
        return ResponseEntity.ok(ApiResponse.success("Restaurant availability updated", r));
    }

    @DeleteMapping("/admin/restaurants/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete restaurant (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<String>> deleteRestaurant(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long id) {
        requireAdmin(token);
        restaurantService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Restaurant deleted", null));
    }

    // ---- Admin: Manage Food Items ----

    @PostMapping("/admin/restaurants/{restaurantId}/items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add food item (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<FoodItem>> addFoodItem(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long restaurantId,
            @Valid @RequestBody FoodItemRequest req) {
        requireAdmin(token);
        FoodItem item = foodItemService.create(restaurantId, req.getName(), req.getDescription(), req.getPrice(), req.getCategory(), req.getImagePath());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Food item created", item));
    }

    @PutMapping("/admin/food-items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update food item (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<FoodItem>> updateFoodItem(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long id,
            @Valid @RequestBody FoodItemRequest req) {
        requireAdmin(token);
        FoodItem item = foodItemService.update(id, req.getName(), req.getDescription(), req.getPrice(), req.getCategory(), req.getAvailable(), req.getImagePath());
        return ResponseEntity.ok(ApiResponse.success("Food item updated", item));
    }

    @DeleteMapping("/admin/food-items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete food item (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<String>> deleteFoodItem(
            @RequestHeader("X-Session-Token") String token, @PathVariable Long id) {
        requireAdmin(token);
        foodItemService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Food item deleted", null));
    }

    // ---- Admin: Manage Orders ----

    @PatchMapping("/admin/orders/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update order status (Admin only)", security = @SecurityRequirement(name = "X-Session-Token"))
    public ResponseEntity<ApiResponse<com.foodieexpress.model.Order>> updateOrderStatus(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long orderId,
            @Valid @RequestBody com.foodieexpress.dto.OrderStatusRequest body) {
        requireAdmin(token);
        com.foodieexpress.model.Order updated = orderService.updateOrderStatus(orderId, body.status());
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }
}
