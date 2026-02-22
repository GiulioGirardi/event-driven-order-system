package com.example.orderservice.application;

import com.example.orderservice.application.port.OrderEventPublisher;
import com.example.orderservice.application.port.OrderRepository;
import com.example.orderservice.domain.Order;
import org.slf4j.MDC;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Creates an order (PENDING), persists it, and publishes OrderCreated.
 * Correlation ID is expected in MDC so the event can propagate it downstream.
 */
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public CreateOrderUseCase(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public UUID execute(String customerId, BigDecimal totalAmount, String currency,
                       List<OrderItemCommand> items) {
        UUID orderId = UUID.randomUUID();
        Instant now = Instant.now();
        List<Order.OrderItem> domainItems = items.stream()
                .map(i -> new Order.OrderItem(i.productId(), i.quantity(), i.unitPrice()))
                .collect(Collectors.toList());
        Order order = new Order(orderId, customerId, totalAmount, currency, domainItems, now);
        orderRepository.save(order);

        String correlationId = MDC.get("correlationId");
        if (correlationId == null) {
            correlationId = orderId.toString();
        }
        String eventId = UUID.randomUUID().toString();
        eventPublisher.publishOrderCreated(new OrderEventPublisher.OrderCreatedEvent(
                eventId,
                correlationId,
                orderId.toString(),
                customerId,
                totalAmount,
                currency,
                items.stream()
                        .map(i -> new OrderEventPublisher.OrderCreatedEvent.OrderItemPayload(
                                i.productId(), i.quantity(), i.unitPrice()))
                        .toList()
        ));
        return orderId;
    }

    public record OrderItemCommand(String productId, int quantity, BigDecimal unitPrice) {}
}
