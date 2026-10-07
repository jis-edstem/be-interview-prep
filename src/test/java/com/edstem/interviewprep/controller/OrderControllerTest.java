package com.edstem.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.edstem.interviewprep.dto.OrderRequest;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.repository.OrderRepository;
import com.edstem.interviewprep.repository.ProductRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class OrderControllerTest {

    private static final String ORDERS = "/api/orders";
    private static final String CUSTOMER = "1";

    private final MockMvcTester mvc;
    private final OrderRepository orders;
    private final ProductRepository products;

    private long productId;

    @BeforeEach
    void saveProduct() {
        orders.deleteAll();
        productId = products.save(new Product("Kettle", "Home", new BigDecimal("30.00"), 2, BigDecimal.ONE))
                .getId();
    }

    @Test
    void placeReturnsCreatedOrder() {
        MvcTestResult result = place(UUID.randomUUID().toString(), items(productId, 2));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).hasHeader("Location", "http://localhost/api/orders/" + id(result));
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PLACED");
        assertThat(result).bodyJson().extractingPath("$.items[0].quantity").isEqualTo(2);
    }

    @Test
    void retryWithSameKeyReturnsTheOriginalOrder() {
        String key = UUID.randomUUID().toString();
        MvcTestResult first = place(key, items(productId, 1));

        MvcTestResult retry = place(key, items(productId, 1));

        assertThat(retry).hasStatus(HttpStatus.CREATED);
        assertThat(retry).bodyJson().extractingPath("$.id").isEqualTo(id(first));
        assertThat(retry).hasHeader("Location", "http://localhost/api/orders/" + id(first));
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(1);
    }

    @Test
    void reusingKeyForDifferentItemsReturnsUnprocessable() {
        String key = UUID.randomUUID().toString();
        place(key, items(productId, 1));

        MvcTestResult result = place(key, items(productId, 2));

        assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Idempotency-Key " + key + " was already used for a different order");
    }

    @Test
    void insufficientStockReturnsConflict() {
        MvcTestResult result = place(UUID.randomUUID().toString(), items(productId, 3));

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Product " + productId + " does not have 3 in stock; no items were reserved");
    }

    @Test
    void missingIdempotencyKeyIsRejected() {
        MvcTestResult result = mvc.post()
                .uri(ORDERS)
                .with(jwt().jwt(token -> token.subject(CUSTOMER)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(productId, 1))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.detail").asString().contains("Idempotency-Key");
    }

    @Test
    void invalidItemsReturnFieldErrors() {
        String body = """
                {"items": [{"productId": %d, "quantity": 0}, {"productId": %d, "quantity": 1}]}
                """.formatted(productId, productId);

        MvcTestResult result = place(UUID.randomUUID().toString(), body);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors[*].field")
                .asArray()
                .containsExactlyInAnyOrder("items[0].quantity", "items");
    }

    @Test
    void tooManyItemsAreRejected() {
        String items = IntStream.rangeClosed(1, OrderRequest.MAX_ITEMS + 1)
                .mapToObj("{\"productId\": %d, \"quantity\": 1}"::formatted)
                .collect(Collectors.joining(","));

        MvcTestResult result = place(UUID.randomUUID().toString(), "{\"items\": [" + items + "]}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("items");
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(2);
    }

    @Test
    void cancelReturnsCancelledOrder() {
        Number id = id(place(UUID.randomUUID().toString(), items(productId, 1)));

        MvcTestResult result = mvc.post()
                .uri(ORDERS + "/{id}/cancel", id)
                .with(jwt().jwt(token -> token.subject(CUSTOMER)))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("CANCELLED");
    }

    @Test
    void anotherCustomersOrderIsNotFound() {
        Number id = id(place(UUID.randomUUID().toString(), items(productId, 1)));

        MvcTestResult result = mvc.get()
                .uri(ORDERS + "/{id}", id)
                .with(jwt().jwt(token -> token.subject("2")))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void anonymousOrderIsUnauthorized() {
        MvcTestResult result = mvc.post()
                .uri(ORDERS)
                .header(OrderController.IDEMPOTENCY_KEY, UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(items(productId, 1))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MvcTestResult place(String idempotencyKey, String body) {
        return mvc.post()
                .uri(ORDERS)
                .with(jwt().jwt(token -> token.subject(CUSTOMER)))
                .header(OrderController.IDEMPOTENCY_KEY, idempotencyKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private static String items(long productId, int quantity) {
        return """
                {"items": [{"productId": %d, "quantity": %d}]}
                """.formatted(productId, quantity);
    }

    private static Number id(MvcTestResult result) {
        return JsonPath.read(new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8), "$.id");
    }
}
