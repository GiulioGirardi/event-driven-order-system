package com.example.paymentservice.infrastructure.kafka;

import com.example.paymentservice.application.ProcessRefundRequestedUseCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes PaymentRefundRequested (Saga compensation from inventory-service); performs refund.
 */
@Component
class PaymentRefundRequestedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentRefundRequestedConsumer.class);

    private final ProcessRefundRequestedUseCase processRefundRequested;
    private final ObjectMapper objectMapper;

    PaymentRefundRequestedConsumer(ProcessRefundRequestedUseCase processRefundRequested,
                                   ObjectMapper objectMapper) {
        this.processRefundRequested = processRefundRequested;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-refund-requested}",
            groupId = "${app.kafka.consumer-groups.payment-refund-requested}",
            containerFactory = "listenerContainerFactory"
    )
    public void onMessage(@Payload String message, @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        try {
            EventEnvelope envelope = objectMapper.readValue(message, EventEnvelope.class);
            String eventId = envelope.getEventId();
            if (envelope.getPayload() == null) {
                log.warn("PaymentRefundRequested missing payload, eventId={}", eventId);
                return;
            }
            JsonNode p = envelope.getPayload();
            String paymentId = p.has("paymentId") ? p.get("paymentId").asText() : null;
            String orderId = p.has("orderId") ? p.get("orderId").asText() : null;
            if (paymentId == null || orderId == null) {
                log.warn("PaymentRefundRequested missing paymentId/orderId, eventId={}", eventId);
                return;
            }
            processRefundRequested.execute(eventId, paymentId, orderId);
        } catch (Exception e) {
            log.error("Error processing PaymentRefundRequested key={}", key, e);
            throw new RuntimeException(e);
        }
    }
}
