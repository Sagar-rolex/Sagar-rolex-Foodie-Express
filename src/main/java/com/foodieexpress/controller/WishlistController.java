package com.foodieexpress.controller;
import com.foodieexpress.dto.*;import com.foodieexpress.model.*;import com.foodieexpress.service.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/wishlist") public class WishlistController {
 private final WishlistService wishlists;private final AuthService auth;private final CustomerService customers;
 public WishlistController(WishlistService w,AuthService a,CustomerService c){wishlists=w;auth=a;customers=c;}
 private Customer customer(String token){User u=auth.validateSession(token).orElseThrow(()->new RuntimeException("Valid customer session required"));if(u.getRole()!=User.Role.CUSTOMER)throw new RuntimeException("Customer access required");return customers.getOrCreateProfile(u);}
 @GetMapping public ResponseEntity<ApiResponse<List<WishlistResponse>>> get(@RequestHeader(value="X-Session-Token",required=false)String token){return ResponseEntity.ok(ApiResponse.success("Wishlist",wishlists.get(customer(token))));}
 @PostMapping("/items/{foodItemId}") public ResponseEntity<ApiResponse<WishlistResponse>> add(@RequestHeader(value="X-Session-Token",required=false)String token,@PathVariable Long foodItemId){Customer c=customer(token);try{return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Favourite saved",wishlists.add(c,foodItemId)));}catch(org.springframework.dao.DataIntegrityViolationException duplicate){return ResponseEntity.ok(ApiResponse.success("Favourite already saved",wishlists.add(c,foodItemId)));}}
 @DeleteMapping("/items/{foodItemId}") public ResponseEntity<ApiResponse<Void>> remove(@RequestHeader(value="X-Session-Token",required=false)String token,@PathVariable Long foodItemId){wishlists.remove(customer(token),foodItemId);return ResponseEntity.ok(ApiResponse.success("Favourite removed",null));}
}
