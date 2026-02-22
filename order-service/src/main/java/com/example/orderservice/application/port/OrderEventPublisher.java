package com.example.orderservice.application.port;

/**
 * Port for publishing order-domain events to Kafka. Keeps application layer
 * unaware of Kafka; infrastructure provides the implementation.
 */
public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderConfirmed(OrderConfirmedEvent event);

    record OrderCreatedEvent(
            String eventId,
            String correlationId,
            String orderId,
            String customerId,
            java.math.BigDecimal totalAmount,
            String currency,
            java.util.List<OrderItemPayload> items
    ) {
        public record OrderItemPayload(String productId, int quantity, java.math.BigDecimal unitPrice) {}
    }

    record OrderConfirmedEvent(
            String eventId,
            String correlationId,
            String orderId,
            String customerId,
            String status,
            String confirmedAt
    ) {}
}
