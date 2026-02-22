package com.example.orderservice.infrastructure.web;

import com.example.orderservice.application.CreateOrderUseCase;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import com.example.orderservice.application.port.OrderRepository;
import com.example.orderservice.domain.Order;

/**
 * REST API for order creation. Generates correlationId for the saga and puts it in MDC
 * so the use case and Kafka events carry it downstream.
 */
@RestController
@RequestMapping("/api/orders")
class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    private final CreateOrderUseCase createOrderUseCase;
    private final OrderRepository orderRepository;

    OrderController(CreateOrderUseCase createOrderUseCase, OrderRepository orderRepository) {
        this.createOrderUseCase = createOrderUseCase;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<CreateOrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
                                                      @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId) {
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("correlationId", correlationId);
        try {
            List<CreateOrderUseCase.OrderItemCommand> items = request.items().stream()
                    .map(i -> new CreateOrderUseCase.OrderItemCommand(i.productId(), i.quantity(), i.unitPrice()))
                    .toList();
            UUID orderId = createOrderUseCase.execute(
                    request.customerId(),
                    request.totalAmount(),
                    request.currency(),
                    items
            );
            log.info("Order created orderId={} customerId={}", orderId, request.customerId());
            return ResponseEntity
                    .accepted()
                    .header(CORRELATION_ID_HEADER, correlationId)
                    .body(new CreateOrderResponse(orderId.toString(), "PENDING", correlationId));
        } finally {
            MDC.remove("correlationId");
        }
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderStatusResponse> getStatus(@PathVariable UUID orderId) {
        return orderRepository.findById(orderId)
                .map(o -> ResponseEntity.ok(new OrderStatusResponse(o.getOrderId().toString(), o.getStatus().name())))
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateOrderResponse(String orderId, String status, String correlationId) {}
    public record OrderStatusResponse(String orderId, String status) {}
}
