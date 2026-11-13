package com.foodieexpress.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "order_status_history", indexes = {
        @Index(name = "idx_order_status_history_order_time", columnList = "order_id, changed_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Order.OrderStatus status;

    @Column(name = "actor_role", nullable = false, length = 32)
    private String actorRole;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @PrePersist
    void setChangedAt() {
        if (changedAt == null) changedAt = LocalDateTime.now();
    }
}
