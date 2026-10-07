package com.edstem.interviewprep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Objects;

public record OrderRequest(
        @NotEmpty(message = "items must not be empty")
        List<@NotNull(message = "item is required") @Valid OrderItemRequest> items) {

    @AssertTrue(message = "items must not repeat a product")
    public boolean isDistinctProducts() {
        if (items == null) {
            return true;
        }
        List<Long> productIds = items.stream()
                .filter(Objects::nonNull)
                .map(OrderItemRequest::productId)
                .toList();
        return productIds.stream().distinct().count() == productIds.size();
    }
}
