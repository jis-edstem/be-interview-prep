package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.Product;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductFilter(
        @Size(max = Product.CATEGORY_MAX_LENGTH, message = "category must be at most {max} characters")
        String category,

        @DecimalMin(value = "0", message = "minPrice must not be negative")
        BigDecimal minPrice,

        @DecimalMin(value = "0", message = "maxPrice must not be negative")
        BigDecimal maxPrice,

        Boolean inStock,

        @Size(max = Product.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
        String name) {

    @AssertTrue(message = "minPrice must not be greater than maxPrice")
    public boolean isValidPriceRange() {
        return minPrice == null || maxPrice == null || minPrice.compareTo(maxPrice) <= 0;
    }

    public boolean inStockOnly() {
        return Boolean.TRUE.equals(inStock);
    }
}
