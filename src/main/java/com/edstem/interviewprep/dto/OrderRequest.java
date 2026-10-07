package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.validation.DistinctProducts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "items must not be empty")
        @Size(max = MAX_ITEMS, message = "items must have at most {max} entries")
        @DistinctProducts(message = "items must not repeat a product")
        List<@NotNull(message = "item is required") @Valid OrderItemRequest> items) {

    public static final int MAX_ITEMS = 100;
}
