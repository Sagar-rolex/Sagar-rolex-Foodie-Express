package com.foodieexpress.controller;
import com.foodieexpress.dto.*;
import com.foodieexpress.model.*;
import com.foodieexpress.service.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/coupons")
public class CouponController {
 private final CouponService coupons; private final AuthService auth; private final CustomerService customers;
 public CouponController(CouponService c,AuthService a,CustomerService cs){coupons=c;auth=a;customers=cs;}
 private Customer requireCustomer(String token){User u=auth.validateSession(token).orElseThrow(()->new RuntimeException("Invalid or missing session token"));if(u.getRole()!=User.Role.CUSTOMER)throw new RuntimeException("Customer access required");return customers.getOrCreateProfile(u);}
 @GetMapping("/offers") public ResponseEntity<ApiResponse<List<Coupon>>> offers(){return ResponseEntity.ok(ApiResponse.success("Available offers",coupons.offers()));}
 @PostMapping("/validate") public ResponseEntity<ApiResponse<CouponTotals>> validate(@RequestHeader(value="X-Session-Token",required=false) String token,@RequestBody Map<String,String> req){return ResponseEntity.ok(ApiResponse.success("Coupon applied",coupons.preview(requireCustomer(token),req.get("code"))));}
}
