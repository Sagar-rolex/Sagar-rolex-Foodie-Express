package com.foodieexpress.model;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="coupon_redemptions", uniqueConstraints=@UniqueConstraint(name="uk_coupon_order", columnNames={"coupon_id","order_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CouponRedemption {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="coupon_id",nullable=false) private Coupon coupon;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false) private Customer customer;
 @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id",nullable=false,unique=true) private Order order;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=16) @Builder.Default private Status status=Status.RESERVED;
 @Column(nullable=false) private LocalDateTime createdAt;
 public enum Status { RESERVED, REDEEMED, RELEASED }
 @PrePersist void init(){ if(createdAt==null) createdAt=LocalDateTime.now(); }
}
