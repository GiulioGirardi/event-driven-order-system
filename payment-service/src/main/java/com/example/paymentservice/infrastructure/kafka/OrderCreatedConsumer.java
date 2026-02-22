package com.example.paymentservice.infrastructure.kafka;

import com.example.paymentservice.application.ProcessOrderCreatedUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Consumes OrderCreated; triggers charge and publishes PaymentConfirmed or PaymentFailed.
 */
@Component
class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProcessOrderCreatedUseCase processOrderCreated;
    private final ObjectMapper objectMapper;

    OrderCreatedConsumer(ProcessOrderCreatedUseCase processOrderCreated, ObjectMapper objectMapper) {
        this.processOrderCreated = processOrderCreated;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            groupId = "${app.kafka.consumer-groups.order-created}",
            containerFactory = "listenerContainerFactory"
    )
    public void onMessage(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String correlationId = null;
        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
            correlationId = envelope.getCorrelationId();
            if (correlationId != null && !correlationId.isEmpty()) {
                MDC.put("correlationId", correlationId);
            }
            String eventId = envelope.getEventId();
            if (envelope.getPayload() == null) {
                log.warn("OrderCreated missing payload, eventId={}", eventId);
                return;
            }
            JsonNode p = envelope.getPayload();
            String orderId = p.has("orderId") ? p.get("orderId").asText() : null;
            String customerId = p.has("customerId") ? p.get("customerId").asText() : null;
            BigDecimal totalAmount = p.has("totalAmount") ? p.get("totalAmount").decimalValue() : null;
            String currency = p.has("currency") ? p.get("currency").asText() : "USD";
            if (orderId == null || customerId == null || totalAmount == null) {
                log.warn("OrderCreated missing required fields, eventId={}", eventId);
                return;
            }
            processOrderCreated.execute(eventId, correlationId != null ? correlationId : "", orderId, customerId, totalAmount, currency);
        } catch (Exception e) {
            log.error("Error processing OrderCreated key={} correlationId={}", key, correlationId, e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
