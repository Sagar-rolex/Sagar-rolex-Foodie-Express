package com.foodieexpress.dto;
import com.foodieexpress.model.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CouponRequest {
 @NotBlank @Size(max=40) private String code;
 @NotNull private Coupon.Type type;
 @NotNull @DecimalMin("0.00") private BigDecimal discountValue;
 @NotNull @DecimalMin("0.00") private BigDecimal minimumOrderAmount;
 @DecimalMin("0.01") private BigDecimal maximumDiscount;
 @NotNull private LocalDateTime startsAt;
 @NotNull private LocalDateTime expiresAt;
 private Boolean active;
 @Min(1) private Integer usageLimit;
 @Min(1) private Integer perUserLimit;
}
