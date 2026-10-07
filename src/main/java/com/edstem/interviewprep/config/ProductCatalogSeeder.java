package com.edstem.interviewprep.config;

import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCatalogSeeder implements ApplicationRunner {

    public static final int SEED_COUNT = 100;

    private static final long RANDOM_SEED = 42;
    private static final List<String> CATEGORIES = List.of("Books", "Electronics", "Home", "Sports", "Toys");
    private static final int MIN_PRICE_CENTS = 199;
    private static final int MAX_PRICE_CENTS = 99_999;
    private static final int MAX_STOCK = 50;
    private static final int MIN_RATING_TENTHS = 10;
    private static final int MAX_RATING_TENTHS = 50;

    private final ProductRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        Random random = new Random(RANDOM_SEED);
        repository.saveAll(IntStream.rangeClosed(1, SEED_COUNT)
                .mapToObj(i -> product(i, random))
                .toList());
    }

    private static Product product(int number, Random random) {
        String category = CATEGORIES.get(number % CATEGORIES.size());
        BigDecimal price = BigDecimal.valueOf(random.nextInt(MIN_PRICE_CENTS, MAX_PRICE_CENTS + 1), 2);
        int stock = random.nextInt(MAX_STOCK + 1);
        BigDecimal rating = BigDecimal.valueOf(random.nextInt(MIN_RATING_TENTHS, MAX_RATING_TENTHS + 1), 1);
        return new Product(category + " item " + number, category, price, stock, rating);
    }
}
