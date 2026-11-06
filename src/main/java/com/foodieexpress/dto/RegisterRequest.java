package com.foodieexpress.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RegisterRequest {
    private String username;
    
    @NotBlank(message = "Password is required") 
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    @NotBlank(message = "Email is required") 
    @Email(message = "Please enter a valid email address")
    private String email;
    
    @NotBlank(message = "Full name is required")
    private String fullName;
    
    private String phone;
    private String address;

    public String getEffectiveUsername() {
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return email != null ? email.trim() : "";
    }
}