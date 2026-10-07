package com.edstem.interviewprep.controller;

import com.edstem.interviewprep.dto.OrderRequest;
import com.edstem.interviewprep.dto.OrderResponse;
import com.edstem.interviewprep.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    private final OrderService service;

    @PostMapping
    public ResponseEntity<OrderResponse> place(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(IDEMPOTENCY_KEY) UUID idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
        OrderResponse order = service.place(customerId(jwt), idempotencyKey, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(order.id())
                .toUri();
        return ResponseEntity.created(location).body(order);
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(customerId(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.cancel(customerId(jwt), id);
    }

    private static Long customerId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
