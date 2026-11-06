package com.foodieexpress.dto;
import com.foodieexpress.model.*;import java.time.LocalDateTime;import java.util.*;
public final class SupportResponses {
 private SupportResponses(){}
 public record Message(Long id,String senderName,String senderRole,String body,LocalDateTime createdAt){}
 public record Event(Long id,String actorName,SupportTicket.Status fromStatus,SupportTicket.Status toStatus,String note,LocalDateTime createdAt){}
 public record Ticket(Long id,String customerName,String customerEmail,Long orderId,SupportTicket.Category category,String subject,String description,SupportTicket.Status status,String assignedAdmin,LocalDateTime createdAt,LocalDateTime updatedAt,List<Message> messages,List<Event> events){}
 public record Faq(Long id,FaqEntry.Category category,String question,String answer,boolean active,LocalDateTime updatedAt){}
}
