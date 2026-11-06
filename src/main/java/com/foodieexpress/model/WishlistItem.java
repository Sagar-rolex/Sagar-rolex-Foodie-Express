package com.foodieexpress.model;
import jakarta.persistence.*;import lombok.*;import java.math.BigDecimal;import java.time.LocalDateTime;
@Entity @Table(name="wishlist_items",uniqueConstraints=@UniqueConstraint(name="uk_wishlist_customer_food",columnNames={"customer_id","food_item_id"}),indexes=@Index(name="idx_wishlist_customer",columnList="customer_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class WishlistItem {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false) private Customer customer;
 // Deliberately stored as an id rather than a mandatory FK so a wishlist remains readable if a dish is hard-deleted.
 @Column(name="food_item_id",nullable=false) private Long foodItemId;
 @Column(length=160) private String savedName;
 @Column(length=160) private String savedImagePath;
 @Column(precision=10,scale=2) private BigDecimal savedPrice;
 @Column(nullable=false) private LocalDateTime createdAt;
 @PrePersist void beforeInsert(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
