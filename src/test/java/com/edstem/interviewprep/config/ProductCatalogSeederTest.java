package com.edstem.interviewprep.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ProductCatalogSeederTest {

    private final ProductCatalogSeeder seeder;
    private final ProductRepository repository;

    @Test
    void seedsOneHundredValidProductsOnlyIntoAnEmptyCatalog() {
        repository.deleteAll();

        seeder.run(new DefaultApplicationArguments());
        seeder.run(new DefaultApplicationArguments());

        assertThat(repository.count()).isEqualTo(ProductCatalogSeeder.SEED_COUNT);
        assertThat(repository.findAll()).allSatisfy(product -> {
            assertThat(product.getPrice()).isPositive();
            assertThat(product.getStock()).isNotNegative();
            assertThat(product.getRating()).isBetween(BigDecimal.ONE, BigDecimal.valueOf(5));
        });
        assertThat(repository.findAll()).extracting(Product::getStock).contains(0);
    }
}
