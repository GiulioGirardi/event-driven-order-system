package com.example.orderservice.infrastructure.kafka;

import com.example.orderservice.application.HandleInventoryReservedUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * Consumes InventoryReserved from inventory.reserved. Idempotency and
 * order confirmation are handled in the use case; correlationId is propagated for logging.
 */
@Component
class InventoryReservedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryReservedConsumer.class);

    private final HandleInventoryReservedUseCase handleInventoryReserved;
    private final ObjectMapper objectMapper;

    InventoryReservedConsumer(HandleInventoryReservedUseCase handleInventoryReserved,
                              ObjectMapper objectMapper) {
        this.handleInventoryReserved = handleInventoryReserved;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.inventory-reserved}",
            groupId = "${app.kafka.consumer-groups.inventory-reserved}",
            containerFactory = "listenerContainerFactory"
    )
    public void onMessage(@Payload String message,
                          @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        String correlationId = null;
        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
            correlationId = envelope.getCorrelationId();
            if (correlationId != null && !correlationId.isEmpty()) {
                org.slf4j.MDC.put("correlationId", correlationId);
            }
            String eventId = envelope.getEventId();
            String orderId = envelope.getPayload() != null && envelope.getPayload().has("orderId")
                    ? envelope.getPayload().get("orderId").asText()
                    : null;
            if (orderId == null) {
                log.warn("InventoryReserved missing orderId in payload, eventId={}", eventId);
                return;
            }
            handleInventoryReserved.execute(eventId, correlationId != null ? correlationId : "", orderId);
        } catch (Exception e) {
            log.error("Error processing InventoryReserved key={} correlationId={}", key, correlationId, e);
            throw new RuntimeException(e);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
