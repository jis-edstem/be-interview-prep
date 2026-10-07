package com.edstem.interviewprep.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS = "products";

    private static final int MAX_CACHED_PRODUCTS = 10_000;
    private static final Duration PRODUCT_TTL = Duration.ofMinutes(10);

    @Bean
    CacheManager cacheManager() {
        CaffeineCacheManager caffeine = new CaffeineCacheManager(PRODUCTS);
        caffeine.setCaffeine(
                Caffeine.newBuilder().maximumSize(MAX_CACHED_PRODUCTS).expireAfterWrite(PRODUCT_TTL));
        return new TransactionAwareCacheManagerProxy(caffeine);
    }
}
