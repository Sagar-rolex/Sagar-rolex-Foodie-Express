package com.foodieexpress.service;

import com.foodieexpress.model.*;
import com.foodieexpress.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CouponServiceTest {
 private CouponRepository coupons; private CouponRedemptionRepository redemptions; private CartRepository carts; private CouponService service;
 @BeforeEach void setup(){coupons=mock(CouponRepository.class);redemptions=mock(CouponRedemptionRepository.class);carts=mock(CartRepository.class);service=new CouponService(coupons,redemptions,carts);}
 private Coupon coupon(Coupon.Type type,String value){return Coupon.builder().id(1L).code("SAVE").type(type).discountValue(new BigDecimal(value)).minimumOrderAmount(new BigDecimal("100.00")).startsAt(LocalDateTime.now().minusDays(1)).expiresAt(LocalDateTime.now().plusDays(1)).active(true).build();}
 @Test void calculatesPercentageWithMaximumAndTax(){Coupon c=coupon(Coupon.Type.PERCENTAGE,"20");c.setMaximumDiscount(new BigDecimal("30.00"));var totals=service.calculate(c,new BigDecimal("200.00"));assertThat(totals.getDiscount()).isEqualByComparingTo("30.00");assertThat(totals.getTax()).isEqualByComparingTo("20.00");assertThat(totals.getGrandTotal()).isEqualByComparingTo("230.00");}
 @Test void fixedDiscountCannotExceedSubtotal(){var totals=service.calculate(coupon(Coupon.Type.FIXED,"500"),new BigDecimal("200.00"));assertThat(totals.getDiscount()).isEqualByComparingTo("200.00");assertThat(totals.getGrandTotal()).isEqualByComparingTo("60.00");}
 @Test void freeDeliveryWaivesExistingDeliveryFee(){var totals=service.calculate(coupon(Coupon.Type.FREE_DELIVERY,"0"),new BigDecimal("200.00"));assertThat(totals.getDeliveryFee()).isEqualByComparingTo("40.00");assertThat(totals.getDiscount()).isEqualByComparingTo("40.00");assertThat(totals.getGrandTotal()).isEqualByComparingTo("220.00");}
 @Test void rejectsExpiredCoupon(){Coupon c=coupon(Coupon.Type.FIXED,"10");c.setExpiresAt(LocalDateTime.now().minusSeconds(1));when(coupons.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));when(carts.findByCustomer(any())).thenReturn(Optional.of(Cart.builder().items(new ArrayList<>()).build()));assertThatThrownBy(()->service.preview(Customer.builder().id(7L).build(),"SAVE")).hasMessageContaining("expired");}
 @Test void rejectsInactiveAndMinimumOrderCoupons(){Coupon c=coupon(Coupon.Type.FIXED,"10");c.setActive(false);when(coupons.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));when(carts.findByCustomer(any())).thenReturn(Optional.of(Cart.builder().items(new ArrayList<>()).build()));assertThatThrownBy(()->service.preview(Customer.builder().id(7L).build(),"SAVE")).hasMessageContaining("inactive");c.setActive(true);FoodItem food=FoodItem.builder().price(new BigDecimal("50.00")).build();when(carts.findByCustomer(any())).thenReturn(Optional.of(Cart.builder().items(new ArrayList<>(List.of(CartItem.builder().foodItem(food).quantity(1).build()))).build()));assertThatThrownBy(()->service.preview(Customer.builder().id(7L).build(),"SAVE")).hasMessageContaining("Minimum order");}
 @Test void rejectsExhaustedGlobalAndPerUserLimits(){Coupon c=coupon(Coupon.Type.FIXED,"10");c.setUsageLimit(1);c.setPerUserLimit(1);when(coupons.findByCodeIgnoreCase("SAVE")).thenReturn(Optional.of(c));FoodItem food=FoodItem.builder().price(new BigDecimal("150.00")).build();Cart cart=Cart.builder().items(new ArrayList<>(List.of(CartItem.builder().foodItem(food).quantity(1).build()))).build();when(carts.findByCustomer(any())).thenReturn(Optional.of(cart));when(redemptions.countByCouponAndStatusIn(eq(c),anyCollection())).thenReturn(1L);assertThatThrownBy(()->service.preview(Customer.builder().id(7L).build(),"SAVE")).hasMessageContaining("usage limit");}
 @Test void rejectsUnknownCoupon(){when(coupons.findByCodeIgnoreCase("NOPE")).thenReturn(Optional.empty());assertThatThrownBy(()->service.preview(Customer.builder().id(7L).build(),"NOPE")).hasMessageContaining("Invalid coupon");}
}
