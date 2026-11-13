package com.foodieexpress.dto;

import com.foodieexpress.model.Cart;

import java.math.BigDecimal;
import java.util.List;

public record ReorderResponse(Cart cart, BigDecimal currentSubtotal, List<String> addedItems, List<String> unavailableItems) {}
