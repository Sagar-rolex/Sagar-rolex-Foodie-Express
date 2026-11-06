package com.foodieexpress.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Table(name="support_tickets",indexes={@Index(name="idx_support_customer",columnList="customer_id"),@Index(name="idx_support_status",columnList="status")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicket {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id",nullable=false) private Customer customer;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="order_id") private Order order;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="assigned_admin_id") private User assignedAdmin;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=24) private Category category;
 @Column(nullable=false,length=120) private String subject;
 @Column(nullable=false,length=3000) private String description;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) @Builder.Default private Status status=Status.OPEN;
 @Column(nullable=false) private LocalDateTime createdAt;
 @Column(nullable=false) private LocalDateTime updatedAt;
 public enum Category { ORDERS, PAYMENTS, DELIVERY, CANCELLATIONS, COUPONS, REFUNDS, OTHER }
 public enum Status { OPEN, IN_PROGRESS, WAITING_FOR_CUSTOMER, RESOLVED }
 @PrePersist void beforeInsert(){LocalDateTime now=LocalDateTime.now();if(createdAt==null)createdAt=now;if(updatedAt==null)updatedAt=now;}
 @PreUpdate void beforeUpdate(){updatedAt=LocalDateTime.now();}
}
