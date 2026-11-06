package com.foodieexpress.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name="coupons", indexes=@Index(name="idx_coupon_code", columnList="code", unique=true))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false, unique=true, length=40) private String code;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Type type;
    @Column(nullable=false, precision=10, scale=2) private BigDecimal discountValue;
    @Column(nullable=false, precision=10, scale=2) @Builder.Default private BigDecimal minimumOrderAmount=BigDecimal.ZERO;
    @Column(precision=10, scale=2) private BigDecimal maximumDiscount;
    @Column(nullable=false) private LocalDateTime startsAt;
    @Column(nullable=false) private LocalDateTime expiresAt;
    @Column(nullable=false) @Builder.Default private boolean active=true;
    private Integer usageLimit;
    private Integer perUserLimit;
    public enum Type { PERCENTAGE, FIXED, FREE_DELIVERY }
}
