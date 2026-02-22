package com.example.paymentservice.application.port;

/**
 * Port for publishing payment-domain events. Infrastructure implements with Kafka.
 */
public interface PaymentEventPublisher {

    void publishPaymentConfirmed(PaymentConfirmedEvent event);

    void publishPaymentFailed(PaymentFailedEvent event);

    record PaymentConfirmedEvent(
            String eventId,
            String correlationId,
            String paymentId,
            String orderId,
            java.math.BigDecimal amount,
            String currency,
            String status
    ) {}

    record PaymentFailedEvent(
            String eventId,
            String correlationId,
            String paymentId,
            String orderId,
            java.math.BigDecimal amount,
            String currency,
            String status,
            String reason
    ) {}
}
