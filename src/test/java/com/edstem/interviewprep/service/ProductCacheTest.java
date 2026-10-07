package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;

import com.edstem.interviewprep.dto.ProductRequest;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.ProductNotFoundException;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class ProductCacheTest {

    private final ProductService service;

    @MockitoSpyBean
    private ProductRepository repository;

    private long id;

    @BeforeEach
    void saveProduct() {
        id = repository
                .save(new Product("Java Guide", "Books", new BigDecimal("25.00"), 3, BigDecimal.valueOf(4)))
                .getId();
        clearInvocations(repository);
    }

    @Test
    void repeatedLookupsQueryTheDatabaseOnce() {
        service.get(id);
        service.get(id);
        service.get(id);

        then(repository).should(times(1)).findById(id);
    }

    @Test
    void lookupAfterUpdateReturnsTheNewValues() {
        service.get(id);

        service.update(
                id, new ProductRequest("Java Guide, 2nd edition", "Books", new BigDecimal("29.50"), 3, BigDecimal.ONE));

        assertThat(service.get(id).name()).isEqualTo("Java Guide, 2nd edition");
        assertThat(service.get(id).price()).isEqualByComparingTo("29.50");
    }

    @Test
    void lookupAfterDeleteReturnsNotFound() {
        service.get(id);

        service.delete(id);

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ProductNotFoundException.class);
    }
}
