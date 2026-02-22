package com.example.orderservice.infrastructure.kafka;

import com.example.orderservice.application.HandleInventoryFailedUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
class InventoryFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryFailedConsumer.class);

    private final HandleInventoryFailedUseCase handleInventoryFailed;
    private final ObjectMapper objectMapper;

    InventoryFailedConsumer(HandleInventoryFailedUseCase handleInventoryFailed,
                            ObjectMapper objectMapper) {
        this.handleInventoryFailed = handleInventoryFailed;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.inventory-failed}",
            groupId = "${app.kafka.consumer-groups.inventory-failed}",
            containerFactory = "listenerContainerFactory"
    )
    public void onMessage(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventId = envelope.getEventId();
            String orderId = envelope.getPayload() != null && envelope.getPayload().has("orderId")
                    ? envelope.getPayload().get("orderId").asText()
                    : null;
            if (orderId == null) {
                log.warn("InventoryFailed missing orderId, eventId={}", eventId);
                return;
            }
            handleInventoryFailed.execute(eventId, orderId);
        } catch (Exception e) {
            log.error("Error processing InventoryFailed key={}", key, e);
            throw new RuntimeException(e);
        }
    }
}
