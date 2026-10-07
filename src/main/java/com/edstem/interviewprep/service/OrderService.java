package com.edstem.interviewprep.service;

import com.edstem.interviewprep.dto.OrderItemRequest;
import com.edstem.interviewprep.dto.OrderRequest;
import com.edstem.interviewprep.dto.OrderResponse;
import com.edstem.interviewprep.entity.Order;
import com.edstem.interviewprep.entity.OrderItem;
import com.edstem.interviewprep.entity.OrderStatus;
import com.edstem.interviewprep.exception.IdempotencyKeyReusedException;
import com.edstem.interviewprep.exception.OrderNotFoundException;
import com.edstem.interviewprep.repository.OrderRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Comparator<OrderItemRequest> PRODUCT_LOCK_ORDER =
            Comparator.comparing(OrderItemRequest::productId);

    private final OrderRepository repository;
    private final ProductService productService;
    private final TransactionTemplate transaction;

    public OrderResponse place(Long customerId, UUID idempotencyKey, OrderRequest request) {
        try {
            return transaction.execute(status -> replay(customerId, idempotencyKey, request)
                    .orElseGet(() -> create(customerId, idempotencyKey, request)));
        } catch (DataIntegrityViolationException e) {
            return replay(customerId, idempotencyKey, request).orElseThrow(() -> e);
        }
    }

    @Transactional(readOnly = true)
    public OrderResponse get(Long customerId, Long id) {
        return repository
                .findByIdAndCustomerId(id, customerId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    @Transactional
    public OrderResponse cancel(Long customerId, Long id) {
        Order order = repository.findForUpdate(id, customerId).orElseThrow(() -> new OrderNotFoundException(id));
        if (order.getStatus() == OrderStatus.PLACED) {
            order.cancel();
            order.getItems().forEach(item -> productService.releaseStock(item.getProductId(), item.getQuantity()));
        }
        return OrderResponse.from(order);
    }

    private OrderResponse create(Long customerId, UUID idempotencyKey, OrderRequest request) {
        List<OrderItem> items = request.items().stream()
                .sorted(PRODUCT_LOCK_ORDER)
                .map(item -> new OrderItem(item.productId(), item.quantity()))
                .toList();
        Order order = repository.saveAndFlush(new Order(customerId, idempotencyKey, items));
        items.forEach(item -> productService.reserveStock(item.getProductId(), item.getQuantity()));
        return OrderResponse.from(order);
    }

    private Optional<OrderResponse> replay(Long customerId, UUID idempotencyKey, OrderRequest request) {
        return repository
                .findByCustomerIdAndIdempotencyKey(customerId, idempotencyKey)
                .map(OrderResponse::from)
                .map(order -> {
                    if (!order.items().equals(requestedItems(request))) {
                        throw new IdempotencyKeyReusedException(idempotencyKey);
                    }
                    return order;
                });
    }

    private static List<OrderResponse.Item> requestedItems(OrderRequest request) {
        return request.items().stream()
                .sorted(PRODUCT_LOCK_ORDER)
                .map(item -> new OrderResponse.Item(item.productId(), item.quantity()))
                .toList();
    }
}
