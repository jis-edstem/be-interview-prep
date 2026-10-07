package com.edstem.interviewprep.repository;

import com.edstem.interviewprep.dto.ProductFilter;
import com.edstem.interviewprep.entity.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ProductSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ProductSpecifications() {}

    public static Specification<Product> matching(ProductFilter filter) {
        List<Specification<Product>> conditions = new ArrayList<>();
        if (StringUtils.hasText(filter.category())) {
            conditions.add((root, query, cb) -> cb.equal(root.get("category"), filter.category()));
        }
        if (filter.minPrice() != null) {
            conditions.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), filter.minPrice()));
        }
        if (filter.maxPrice() != null) {
            conditions.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), filter.maxPrice()));
        }
        if (filter.inStockOnly()) {
            conditions.add((root, query, cb) -> cb.greaterThan(root.get("stock"), 0));
        }
        if (StringUtils.hasText(filter.name())) {
            String pattern = "%" + escapeLike(filter.name().toLowerCase(Locale.ROOT)) + "%";
            conditions.add((root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE));
        }
        return Specification.allOf(conditions);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
