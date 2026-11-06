package com.foodieexpress.controller;
import com.foodieexpress.dto.*;import com.foodieexpress.model.Coupon;import com.foodieexpress.service.*;
import jakarta.validation.Valid;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.*;
@RestController @RequestMapping("/api/admin/coupons") @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
public class AdminCouponController {
 private final AdminAuthorization authorization;private final CouponService coupons;
 public AdminCouponController(AdminAuthorization a,CouponService c){authorization=a;coupons=c;}
 @GetMapping public ResponseEntity<ApiResponse<List<Map<String,Object>>>> list(@RequestHeader(value="X-Session-Token",required=false)String token){authorization.requireAdmin(token);return ResponseEntity.ok(ApiResponse.success("Coupons",coupons.adminList()));}
 @PostMapping public ResponseEntity<ApiResponse<Coupon>> create(@RequestHeader(value="X-Session-Token",required=false)String token,@Valid @RequestBody CouponRequest req){authorization.requireAdmin(token);return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Coupon created",coupons.save(null,req)));}
 @PutMapping("/{id}") public ResponseEntity<ApiResponse<Coupon>> update(@RequestHeader(value="X-Session-Token",required=false)String token,@PathVariable Long id,@Valid @RequestBody CouponRequest req){authorization.requireAdmin(token);return ResponseEntity.ok(ApiResponse.success("Coupon updated",coupons.save(id,req)));}
 @PatchMapping("/{id}/active") public ResponseEntity<ApiResponse<Coupon>> active(@RequestHeader(value="X-Session-Token",required=false)String token,@PathVariable Long id,@RequestBody Map<String,Boolean> body){authorization.requireAdmin(token);if(body.get("active")==null)return ResponseEntity.badRequest().body(ApiResponse.error("active must be true or false"));return ResponseEntity.ok(ApiResponse.success("Coupon status updated",coupons.setActive(id,body.get("active"))));}
}
