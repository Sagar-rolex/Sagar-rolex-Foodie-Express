package com.foodieexpress.dto;
import com.foodieexpress.model.SupportTicket;import jakarta.validation.constraints.*;
public final class SupportRequests {
 private SupportRequests(){}
 public record CreateTicket(@NotNull SupportTicket.Category category,@NotBlank @Size(max=120) String subject,@NotBlank @Size(max=3000) String description,Long orderId){}
 public record Reply(@NotBlank @Size(max=4000) String message){}
 public record StatusUpdate(@NotNull SupportTicket.Status status,@Size(max=500) String note){}
 public record Assignment(@NotNull @Positive Long adminId){}
 public record Faq(@NotNull com.foodieexpress.model.FaqEntry.Category category,@NotBlank @Size(max=240) String question,@NotBlank @Size(max=5000) String answer,Boolean active){}
}
