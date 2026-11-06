package com.foodieexpress.dto;
import java.math.BigDecimal;import java.time.LocalDateTime;
public record WishlistResponse(Long id,Long foodItemId,String name,String imagePath,BigDecimal price,Double rating,Long restaurantId,String restaurantName,boolean available,boolean removed,LocalDateTime savedAt){}
