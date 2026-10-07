package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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

    @Test
    void allFiltersCombineInOneRequest() {
        repository.saveAll(List.of(
                product("Java Guide", "Books", "25.00", 3),
                product("Python Guide", "Books", "40.00", 0),
                product("Cooking Guide", "Books", "80.00", 5),
                product("Java Novel", "Books", "15.00", 2),
                product("Guide Lamp", "Home", "30.00", 4)));

        MvcTestResult result = mvc.get()
                .uri(PRODUCTS)
                .param("category", "Books")
                .param("minPrice", "20")
                .param("maxPrice", "50")
                .param("inStock", "true")
                .param("name", "GUIDE")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.content[*].name")
                .asArray()
                .containsExactly("Java Guide");
        assertThat(result).bodyJson().extractingPath("$.totalElements").isEqualTo(1);
    }

    @Test
    void nameSearchTreatsWildcardsLiterally() {
        repository.saveAll(
                List.of(product("100% Cotton Towel", "Home", "9.99", 1), product("Plain Towel", "Home", "7.99", 1)));

        MvcTestResult result = mvc.get().uri(PRODUCTS).param("name", "%").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result)
                .bodyJson()
                .extractingPath("$.content[*].name")
                .asArray()
                .containsExactly("100% Cotton Towel");
    }

    @Test
    void invalidPriceFiltersAreRejected() {
        MvcTestResult negative = mvc.get().uri(PRODUCTS).param("minPrice", "-1").exchange();
        MvcTestResult inverted = mvc.get()
                .uri(PRODUCTS)
                .param("minPrice", "50")
                .param("maxPrice", "10")
                .exchange();

        assertThat(negative).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(negative)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("minPrice must not be negative");
        assertThat(inverted).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(inverted)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("minPrice must not be greater than maxPrice");
    }

    @Test
    void unconvertibleFilterValueIsRejectedWithFieldMessage() {
        MvcTestResult result = mvc.get().uri(PRODUCTS).param("minPrice", "abc").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("minPrice");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[0].message")
                .isEqualTo("minPrice has an invalid value 'abc'");
    }

    @Test
    void getReturnsProductOrNotFound() {
        long id = repository.save(product("Java Guide", "Books", "25.00", 3)).getId();

        assertThat(mvc.get().uri(PRODUCTS + "/" + id))
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Java Guide");
        assertThat(mvc.get().uri(PRODUCTS + "/999999")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminUpdatesProduct() {
        long id = repository.save(product("Java Guide", "Books", "25.00", 3)).getId();

        MvcTestResult result = putProduct(id, productJson("Java Guide, 2nd edition", "29.50"));

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Java Guide, 2nd edition");
        assertThat(repository.findById(id).orElseThrow().getPrice()).isEqualByComparingTo("29.50");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminDeletesProduct() {
        long id = repository.save(product("Java Guide", "Books", "25.00", 3)).getId();

        assertThat(mvc.delete().uri(PRODUCTS + "/" + id)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(repository.existsById(id)).isFalse();
    }

    @Test
    void userCannotChangeProducts() {
        long id = repository.save(product("Java Guide", "Books", "25.00", 3)).getId();

        assertThat(putProduct(id, productJson("Renamed", "1.00"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mvc.delete().uri(PRODUCTS + "/" + id)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidUpdateReturnsFieldErrors() {
        long id = repository.save(product("Java Guide", "Books", "25.00", 3)).getId();

        MvcTestResult result = putProduct(id, """
                {"name": "", "category": "Books", "price": -1, "stock": -5, "rating": 7}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("name", "price", "stock", "rating");
    }

    private MvcTestResult putProduct(long id, String body) {
        return mvc.put()
                .uri(PRODUCTS + "/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String productJson(String name, String price) {
        return """
                {"name": "%s", "category": "Books", "price": %s, "stock": 3, "rating": 4.5}
                """.formatted(name, price);
    }

    private static Product product(String name, String category, String price, int stock) {
        return new Product(name, category, new BigDecimal(price), stock, BigDecimal.valueOf(4));
    }

    private void saveProducts(int count) {
        repository.saveAll(IntStream.rangeClosed(1, count)
                .mapToObj(i -> new Product("Product " + i, "Books", BigDecimal.valueOf(i), i, BigDecimal.valueOf(4)))
                .toList());
    }
}
