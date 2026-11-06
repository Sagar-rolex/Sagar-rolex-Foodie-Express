package com.foodieexpress.dto;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponTotals {
 private String code; private String type; private BigDecimal subtotal; private BigDecimal tax;
 private BigDecimal deliveryFee; private BigDecimal discount; private BigDecimal grandTotal;
 private String description; private BigDecimal minimumOrderAmount; private LocalDateTimeHolder validity;
 @Getter @Setter @NoArgsConstructor @AllArgsConstructor public static class LocalDateTimeHolder { private String startsAt; private String expiresAt; }
}
