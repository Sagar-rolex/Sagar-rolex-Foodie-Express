package com.foodieexpress.controller;

import com.foodieexpress.dto.ApiResponse;
import com.foodieexpress.dto.CartRequest;
import com.foodieexpress.model.Cart;
import com.foodieexpress.model.Customer;
import com.foodieexpress.model.User;
import com.foodieexpress.service.AuthService;
import com.foodieexpress.service.CartService;
import com.foodieexpress.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@SecurityRequirement(name = "X-Session-Token")
@Tag(name = "Cart", description = "Shopping cart - add, update, remove, and view items")
public class CartController {

    @Autowired private CartService cartService;
    @Autowired private AuthService authService;
    @Autowired private CustomerService customerService;

    private Customer requireCustomer(String token) {
        User user = authService.validateSession(token)
                .orElseThrow(() -> new RuntimeException("Invalid or missing session token"));
        if (user.getRole() != User.Role.CUSTOMER) throw new RuntimeException("Customer access required");
        return customerService.getOrCreateProfile(user);
    }

    @GetMapping
    @Operation(summary = "View cart")
    public ResponseEntity<ApiResponse<Map<String, Object>>> viewCart(@RequestHeader("X-Session-Token") String token) {
        Customer customer = requireCustomer(token);
        Cart cart = cartService.getCart(customer);
        BigDecimal total = cartService.calculateTotal(cart);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cart", cart);
        payload.put("total", total);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved", payload));
    }

    @PostMapping("/add")
    @Operation(summary = "Add food item to cart")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addItem(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody CartRequest req) {
        Customer customer = requireCustomer(token);
        Cart cart = cartService.addItem(customer, req.getFoodItemId(), req.getQuantity());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cart", cart);
        payload.put("total", cartService.calculateTotal(cart));
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(ApiResponse.success("Item added to cart", payload));
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge a validated guest cart into the current user's cart")
    public ResponseEntity<ApiResponse<com.foodieexpress.dto.CartMergeResponse>> mergeGuestCart(
            @RequestHeader("X-Session-Token") String token,
            @Valid @RequestBody com.foodieexpress.dto.CartMergeRequest request) {
        Customer customer = requireCustomer(token);
        return ResponseEntity.ok(ApiResponse.success("Guest cart merged", cartService.mergeItems(customer, request.items())));
    }

    @PutMapping("/items/{cartItemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateItem(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        Customer customer = requireCustomer(token);
        Cart cart = cartService.updateQuantity(customer, cartItemId, quantity);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cart", cart);
        payload.put("total", cartService.calculateTotal(cart));
        return ResponseEntity.ok(ApiResponse.success("Cart item updated", payload));
    }

    @DeleteMapping("/items/{cartItemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeItem(
            @RequestHeader("X-Session-Token") String token,
            @PathVariable Long cartItemId) {
        Customer customer = requireCustomer(token);
        Cart cart = cartService.removeItem(customer, cartItemId);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("cart", cart);
        payload.put("total", cartService.calculateTotal(cart));
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart", payload));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear entire cart")
    public ResponseEntity<ApiResponse<String>> clearCart(@RequestHeader("X-Session-Token") String token) {
        Customer customer = requireCustomer(token);
        cartService.clearCart(customer);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared", null));
    }
}
