package com.edstem.interviewprep.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "products",
        indexes = {
            @Index(name = "idx_products_category", columnList = "category"),
            @Index(name = "idx_products_price", columnList = "price")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    public static final int NAME_MAX_LENGTH = 200;
    public static final int CATEGORY_MAX_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, length = CATEGORY_MAX_LENGTH)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private int stock;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;

    public Product(String name, String category, BigDecimal price, int stock, BigDecimal rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }

    public void update(String name, String category, BigDecimal price, int stock, BigDecimal rating) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.rating = rating;
    }
}
