package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@WithMockUser
@RequiredArgsConstructor
class ProductControllerTest {

    private static final String PRODUCTS = "/api/products";

    private final MockMvcTester mvc;
    private final ProductRepository repository;

    @BeforeEach
    void clearProducts() {
        repository.deleteAll();
    }

    @Test
    void listReturnsPageWithTotals() {
        saveProducts(25);

        MvcTestResult result =
                mvc.get().uri(PRODUCTS).param("page", "1").param("size", "10").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.content").asArray().hasSize(10);
        assertThat(result).bodyJson().extractingPath("$.page").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(10);
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(25);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(3);
    }

    @Test
    void pageSizeIsCappedAtOneHundred() {
        saveProducts(150);

        MvcTestResult result = mvc.get().uri(PRODUCTS).param("size", "500").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.size").isEqualTo(100);
        assertThat(result).bodyJson().extractingPath("$.content").asArray().hasSize(100);
        assertThat(result).bodyJson().extractingPath("$.totalPages").isEqualTo(2);
    }

    @Test
    void listSortsByAnyField() {
        saveProducts(5);

        MvcTestResult result =
                mvc.get().uri(PRODUCTS).param("sort", "price,desc").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.content[*].price")
                .asArray()
                .containsExactly(5.0, 4.0, 3.0, 2.0, 1.0);
    }

    @Test
    void unknownSortFieldIsRejected() {
        MvcTestResult result = mvc.get().uri(PRODUCTS).param("sort", "colour").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("sort");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("sort has an unknown property 'colour'");
    }

    private void saveProducts(int count) {
        repository.saveAll(IntStream.rangeClosed(1, count)
                .mapToObj(i -> new Product("Product " + i, "Books", BigDecimal.valueOf(i), i, BigDecimal.valueOf(4)))
                .toList());
    }
}
