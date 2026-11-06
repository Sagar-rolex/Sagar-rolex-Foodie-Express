package com.foodieexpress.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String sessionToken;
    private String message;
}