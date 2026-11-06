package com.foodieexpress.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Table(name="support_ticket_events",indexes=@Index(name="idx_support_event_ticket",columnList="ticket_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportTicketEvent {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ticket_id",nullable=false) private SupportTicket ticket;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_id",nullable=false) private User actor;
 @Enumerated(EnumType.STRING) @Column(length=32) private SupportTicket.Status fromStatus;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private SupportTicket.Status toStatus;
 @Column(length=500) private String note;
 @Column(nullable=false) private LocalDateTime createdAt;
 @PrePersist void beforeInsert(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
