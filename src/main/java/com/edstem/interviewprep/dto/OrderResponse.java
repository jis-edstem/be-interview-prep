package com.edstem.interviewprep.dto;

import com.edstem.interviewprep.entity.Order;
import com.edstem.interviewprep.entity.OrderItem;
import com.edstem.interviewprep.entity.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, List<Item> items, Instant createdAt) {

    public record Item(Long productId, int quantity) {

        static Item from(OrderItem item) {
            return new Item(item.getProductId(), item.getQuantity());
        }
    }

    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getStatus(),
                order.getItems().stream().map(Item::from).toList(),
                order.getCreatedAt());
    }
}
