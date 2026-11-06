package com.foodieexpress.model;
import jakarta.persistence.*;import lombok.*;import java.time.LocalDateTime;
@Entity @Table(name="support_messages",indexes=@Index(name="idx_support_message_ticket",columnList="ticket_id,created_at"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SupportMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="ticket_id",nullable=false) private SupportTicket ticket;
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="sender_id",nullable=false) private User sender;
 @Column(nullable=false,length=4000) private String body;
 @Column(nullable=false) private LocalDateTime createdAt;
 @PrePersist void beforeInsert(){if(createdAt==null)createdAt=LocalDateTime.now();}
}
