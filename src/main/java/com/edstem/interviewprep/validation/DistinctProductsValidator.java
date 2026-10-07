package com.edstem.interviewprep.validation;

import com.edstem.interviewprep.dto.OrderItemRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;

public class DistinctProductsValidator implements ConstraintValidator<DistinctProducts, List<OrderItemRequest>> {

    @Override
    public boolean isValid(List<OrderItemRequest> items, ConstraintValidatorContext context) {
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
