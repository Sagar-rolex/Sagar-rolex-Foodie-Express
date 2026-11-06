package com.foodieexpress.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CartRequest {
    @NotNull
    private Long foodItemId;
    @Min(1)
    private int quantity = 1;
}