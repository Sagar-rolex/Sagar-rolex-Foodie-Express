package com.foodieexpress.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RestaurantRequest {
    @NotBlank
    private String name;
    private String address;
    private String cuisineType;
    private String description;
    private Double rating;
    private Integer deliveryTime;
    @Size(max = 160)
    @Pattern(regexp = "^$|^/images/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:jpg|jpeg|png|webp)$", message = "Image path must point to a local /images asset")
    private String imagePath;
    @Size(max = 180)
    @Pattern(regexp = "^$|^/images/[A-Za-z0-9_-]+(?:/[A-Za-z0-9_-]+)*\\.(?:jpg|jpeg|png|webp)$|^/uploads/restaurants/[a-fA-F0-9-]+\\.(?:jpg|png|webp)$", message = "Banner must be a local image or an uploaded restaurant banner")
    private String bannerImagePath;
}
