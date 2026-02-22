package com.example.orderservice.infrastructure.kafka;

import com.example.orderservice.application.HandlePaymentFailedUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
class PaymentFailedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentFailedConsumer.class);

    private final HandlePaymentFailedUseCase handlePaymentFailed;
    private final ObjectMapper objectMapper;

    PaymentFailedConsumer(HandlePaymentFailedUseCase handlePaymentFailed, ObjectMapper objectMapper) {
        this.handlePaymentFailed = handlePaymentFailed;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.payment-failed}",
            groupId = "${app.kafka.consumer-groups.payment-failed}",
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
                log.warn("PaymentFailed missing orderId, eventId={}", eventId);
                return;
            }
            handlePaymentFailed.execute(eventId, orderId);
        } catch (Exception e) {
            log.error("Error processing PaymentFailed key={}", key, e);
            throw new RuntimeException(e);
        }
    }
}
