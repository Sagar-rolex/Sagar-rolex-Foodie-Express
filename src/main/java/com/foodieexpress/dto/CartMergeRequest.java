package com.foodieexpress.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

public record CartMergeRequest(@NotEmpty @Valid List<Item> items) {
    public record Item(@NotNull Long foodItemId, @Min(1) @Max(99) int quantity,
                       @DecimalMin("0.00") BigDecimal priceAtSave) {}
}
