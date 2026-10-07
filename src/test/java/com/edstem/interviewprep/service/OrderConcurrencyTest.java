package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.dto.OrderItemRequest;
import com.edstem.interviewprep.dto.OrderRequest;
import com.edstem.interviewprep.dto.OrderResponse;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.InsufficientStockException;
import com.edstem.interviewprep.repository.OrderRepository;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class OrderConcurrencyTest {

    private static final int STOCK = 10;
    private static final int CUSTOMERS = 50;
    private static final int RETRIES = 20;

    private final OrderService service;
    private final OrderRepository orders;
    private final ProductRepository products;

    private long productId;

    @BeforeEach
    void saveProduct() {
        orders.deleteAll();
        productId = products.save(
                        new Product("Console", "Electronics", new BigDecimal("499.00"), STOCK, BigDecimal.ONE))
                .getId();
    }

    @Test
    void fiftySimultaneousOrdersSellExactlyTheStock() {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(productId, 1)));

        List<CompletableFuture<OrderResponse>> results =
                runSimultaneously(CUSTOMERS, customer -> service.place((long) customer, UUID.randomUUID(), request));

        assertThat(results.stream().filter(result -> !result.isCompletedExceptionally()))
                .hasSize(STOCK);
        assertThat(results.stream().filter(CompletableFuture::isCompletedExceptionally))
                .hasSize(CUSTOMERS - STOCK)
                .allSatisfy(result -> assertThat(result)
                        .failsWithin(Duration.ZERO)
                        .withThrowableOfType(ExecutionException.class)
                        .withCauseInstanceOf(InsufficientStockException.class));
        assertThat(products.findById(productId).orElseThrow().getStock()).isZero();
        assertThat(orders.count()).isEqualTo(STOCK);
    }

    @Test
    void simultaneousRetriesWithOneKeyCreateOneOrder() {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(productId, 2)));
        UUID key = UUID.randomUUID();

        List<CompletableFuture<OrderResponse>> results =
                runSimultaneously(RETRIES, attempt -> service.place(1L, key, request));

        assertThat(results.stream()
                        .map(CompletableFuture::join)
                        .map(OrderResponse::id)
                        .distinct())
                .hasSize(1);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(STOCK - 2);
    }

    @Test
    void simultaneousCancelsReturnStockOnce() {
        long orderId = service.place(
                        1L, UUID.randomUUID(), new OrderRequest(List.of(new OrderItemRequest(productId, 3))))
                .id();

        List<CompletableFuture<OrderResponse>> results =
                runSimultaneously(RETRIES, attempt -> service.cancel(1L, orderId));

        assertThat(results).allSatisfy(result -> assertThat(result).isCompleted());
        assertThat(products.findById(productId).orElseThrow().getStock()).isEqualTo(STOCK);
    }

    private static <T> List<CompletableFuture<T>> runSimultaneously(int count, IntFunction<T> call) {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService pool = Executors.newFixedThreadPool(count)) {
            List<CompletableFuture<T>> results = IntStream.range(0, count)
                    .mapToObj(i -> CompletableFuture.supplyAsync(
                            () -> {
                                awaitStart(start);
                                return call.apply(i);
                            },
                            pool))
                    .toList();
            start.countDown();
            return results;
        }
    }

    private static void awaitStart(CountDownLatch start) {
        try {
            start.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted before ordering", e);
        }
    }
}
