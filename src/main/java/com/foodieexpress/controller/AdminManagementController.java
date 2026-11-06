package com.foodieexpress.controller;

import com.foodieexpress.dto.*;
import com.foodieexpress.model.*;
import com.foodieexpress.service.AdminAuthorization;
import com.foodieexpress.service.AdminManagementService;
import com.foodieexpress.service.FoodImageStorageService;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminManagementController {
    private final AdminAuthorization authorization;
    private final AdminManagementService management;
    private final FoodImageStorageService imageStorage;

    public AdminManagementController(AdminAuthorization authorization, AdminManagementService management, FoodImageStorageService imageStorage) {
        this.authorization = authorization; this.management = management; this.imageStorage = imageStorage;
    }

    @GetMapping("/restaurants")
    public ResponseEntity<ApiResponse<java.util.List<Restaurant>>> restaurants(@RequestHeader(value="X-Session-Token", required=false) String token) {
        authorization.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.success("Restaurants", management.listRestaurants()));
    }

    @GetMapping("/food-items")
    public ResponseEntity<ApiResponse<Page<FoodItem>>> foodItems(@RequestHeader(value="X-Session-Token", required=false) String token,
            @RequestParam(required=false) String search, @RequestParam(required=false) Long restaurantId,
            @RequestParam(required=false) Boolean available, @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) {
        authorization.requireAdmin(token);
        Pageable pageable = page(page, size, "id");
        return ResponseEntity.ok(ApiResponse.success("Food items", management.listFood(search, restaurantId, available, pageable)));
    }

    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<AdminOrderResponse>>> orders(@RequestHeader(value="X-Session-Token", required=false) String token,
            @RequestParam(required=false) String search, @RequestParam(required=false) Order.OrderStatus status,
            @RequestParam(required=false) Bill.PaymentStatus paymentStatus,
            @RequestParam(required=false) Long restaurantId,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        authorization.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.success("Orders", management.listOrders(search, status, paymentStatus, restaurantId, from, to, page(page,size,"orderTime"))));
    }

    @GetMapping("/customers")
    public ResponseEntity<ApiResponse<Page<AdminCustomerResponse>>> customers(@RequestHeader(value="X-Session-Token", required=false) String token,
            @RequestParam(required=false) String search, @RequestParam(required=false) Boolean active,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="20") int size) {
        authorization.requireAdmin(token);
        return ResponseEntity.ok(ApiResponse.success("Customers", management.listCustomers(search, active, page(page,size,"id"))));
    }

    @PatchMapping("/customers/{id}/active")
    public ResponseEntity<ApiResponse<AdminCustomerResponse>> setCustomerActive(@RequestHeader(value="X-Session-Token", required=false) String token,
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        User actor = authorization.requireAdmin(token);
        Boolean active = body.get("active");
        if (active == null) return ResponseEntity.badRequest().body(ApiResponse.error("active must be true or false"));
        return ResponseEntity.ok(ApiResponse.success("Customer account updated", management.setCustomerActive(id, active, actor)));
    }

    @PostMapping(value="/uploads/foods", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String,String>>> uploadFoodImage(@RequestHeader(value="X-Session-Token", required=false) String token,
            @RequestPart("file") @NotNull MultipartFile file) {
        authorization.requireAdmin(token);
        String path = imageStorage.store(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Image uploaded", Map.of("imagePath", path)));
    }

    @PostMapping(value="/uploads/restaurant-banners", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String,String>>> uploadRestaurantBanner(@RequestHeader(value="X-Session-Token", required=false) String token,
            @RequestPart("file") @NotNull MultipartFile file) {
        authorization.requireAdmin(token);
        String path = imageStorage.storeRestaurantBanner(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Restaurant banner uploaded", Map.of("bannerImagePath", path)));
    }

    private Pageable page(int page, int size, String sort) {
        if (page < 0 || size < 1) throw new RuntimeException("Page must be non-negative and size must be positive");
        return PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, sort));
    }
}
