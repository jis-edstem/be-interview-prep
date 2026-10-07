package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.Product;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductRequest(
        @NotBlank(message = "name is required")
        @Size(max = Product.NAME_MAX_LENGTH, message = "name must be at most {max} characters")
        String name,

        @NotBlank(message = "category is required")
        @Size(max = Product.CATEGORY_MAX_LENGTH, message = "category must be at most {max} characters")
        String category,

        @NotNull(message = "price is required")
        @DecimalMin(value = "0.01", message = "price must be at least {value}")
        @Digits(integer = 8, fraction = 2, message = "price must have at most 2 decimal places")
        BigDecimal price,

        @NotNull(message = "stock is required") @Min(value = 0, message = "stock must not be negative")
        Integer stock,

        @NotNull(message = "rating is required")
        @DecimalMin(value = "0.0", message = "rating must be between 0 and 5")
        @DecimalMax(value = "5.0", message = "rating must be between 0 and 5")
        @Digits(integer = 1, fraction = 1, message = "rating must have at most 1 decimal place")
        BigDecimal rating) {}
