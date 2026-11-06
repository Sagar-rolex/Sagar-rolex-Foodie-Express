package com.foodieexpress.dto;

import com.foodieexpress.model.Cart;
import java.math.BigDecimal;
import java.util.List;

public record CartMergeResponse(Cart cart, BigDecimal total, List<Long> mergedFoodItemIds,
                                List<Long> skippedFoodItemIds, List<Long> priceChangedFoodItemIds) {}
