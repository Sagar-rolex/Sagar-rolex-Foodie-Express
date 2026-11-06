package com.foodieexpress.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class OrderRequest {
    @NotBlank
    private String deliveryAddress;
    private String couponCode;
}
