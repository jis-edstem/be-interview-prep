package com.edstem.interviewprep.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edstem.interviewprep.dto.OrderItemRequest;
import com.edstem.interviewprep.dto.OrderRequest;
import com.edstem.interviewprep.dto.OrderResponse;
import com.edstem.interviewprep.entity.OrderStatus;
import com.edstem.interviewprep.entity.Product;
import com.edstem.interviewprep.exception.IdempotencyKeyReusedException;
import com.edstem.interviewprep.exception.InsufficientStockException;
import com.edstem.interviewprep.exception.ProductNotFoundException;
import com.edstem.interviewprep.repository.OrderRepository;
import com.edstem.interviewprep.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class OrderServiceTest {

    private static final long CUSTOMER = 1L;
    private static final long OTHER_CUSTOMER = 2L;

    private final OrderService service;
    private final OrderRepository orders;
    private final ProductRepository products;

    private long book;
    private long lamp;

    @BeforeEach
    void saveProducts() {
        orders.deleteAll();
        book = save("Book", 5);
        lamp = save("Lamp", 1);
    }

    @Test
    void placingAnOrderReservesEveryItem() {
        OrderResponse order = service.place(CUSTOMER, UUID.randomUUID(), request(item(book, 2), item(lamp, 1)));

        assertThat(order.status()).isEqualTo(OrderStatus.PLACED);
        assertThat(stock(book)).isEqualTo(3);
        assertThat(stock(lamp)).isZero();
    }

    @Test
    void orderWithOneShortItemReservesNothing() {
        OrderRequest request = request(item(book, 2), item(lamp, 2));

        assertThatThrownBy(() -> service.place(CUSTOMER, UUID.randomUUID(), request))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Product " + lamp);
        assertThat(stock(book)).isEqualTo(5);
        assertThat(stock(lamp)).isEqualTo(1);
        assertThat(orders.count()).isZero();
    }

    @Test
    void orderForUnknownProductReservesNothing() {
        OrderRequest request = request(item(book, 1), item(Long.MAX_VALUE, 1));

        assertThatThrownBy(() -> service.place(CUSTOMER, UUID.randomUUID(), request))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(stock(book)).isEqualTo(5);
    }

    @Test
    void retryWithSameKeyReturnsTheOriginalOrder() {
        UUID key = UUID.randomUUID();
        OrderResponse first = service.place(CUSTOMER, key, request(item(book, 2)));

        OrderResponse retry = service.place(CUSTOMER, key, request(item(book, 2)));

        assertThat(retry).isEqualTo(first);
        assertThat(orders.count()).isEqualTo(1);
        assertThat(stock(book)).isEqualTo(3);
    }

    @Test
    void sameKeyFromAnotherCustomerIsANewOrder() {
        UUID key = UUID.randomUUID();
        OrderResponse first = service.place(CUSTOMER, key, request(item(book, 1)));

        OrderResponse second = service.place(OTHER_CUSTOMER, key, request(item(book, 1)));

        assertThat(second.id()).isNotEqualTo(first.id());
        assertThat(stock(book)).isEqualTo(3);
    }

    @Test
    void sameKeyWithDifferentItemsIsRejected() {
        UUID key = UUID.randomUUID();
        service.place(CUSTOMER, key, request(item(book, 1)));

        assertThatThrownBy(() -> service.place(CUSTOMER, key, request(item(book, 2))))
                .isInstanceOf(IdempotencyKeyReusedException.class);
        assertThat(stock(book)).isEqualTo(4);
    }

    private long save(String name, int stock) {
        return products.save(new Product(name, "Home", new BigDecimal("10.00"), stock, BigDecimal.ONE))
                .getId();
    }

    private int stock(long productId) {
        return products.findById(productId).orElseThrow().getStock();
    }

    private static OrderItemRequest item(long productId, int quantity) {
        return new OrderItemRequest(productId, quantity);
    }

    private static OrderRequest request(OrderItemRequest... items) {
        return new OrderRequest(List.of(items));
    }
}
