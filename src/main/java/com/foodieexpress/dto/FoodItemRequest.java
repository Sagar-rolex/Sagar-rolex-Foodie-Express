package com.foodieexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FoodItemRequest {
    @NotBlank @Size(max = 120)
    private String name;
    @Size(max = 2000)
    private String description;
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 8, fraction = 2)
    private BigDecimal price;
    @NotBlank @Size(max = 80)
    private String category;
    private Boolean available;
    @Size(max = 160)
    @Pattern(regexp = "^$|^/images/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:jpg|jpeg|png|webp)$|^/uploads/foods/[a-fA-F0-9-]+\\.(?:jpg|jpeg|png|webp)$", message = "Image must be a project asset or an uploaded food image")
    private String imagePath;
}
