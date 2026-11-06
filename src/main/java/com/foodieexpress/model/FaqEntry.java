package com.foodieexpress.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Table(name="faq_entries",indexes=@Index(name="idx_faq_category_active",columnList="category,active"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FaqEntry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private Category category;
 @Column(nullable=false,length=240) private String question;
 @Column(nullable=false,length=5000) private String answer;
 @Column(nullable=false) @Builder.Default private boolean active=true;
 @Column(nullable=false) private LocalDateTime updatedAt;
 public enum Category { ORDERS, PAYMENTS, DELIVERY, CANCELLATIONS, COUPONS, REFUNDS }
 @PrePersist @PreUpdate void touch(){updatedAt=LocalDateTime.now();}
}
