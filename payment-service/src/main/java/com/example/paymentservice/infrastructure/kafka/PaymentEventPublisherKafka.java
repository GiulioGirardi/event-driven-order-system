package com.example.paymentservice.infrastructure.kafka;

import com.example.paymentservice.application.port.PaymentEventPublisher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
class PaymentEventPublisherKafka implements PaymentEventPublisher {

    private static final String PAYMENT_CONFIRMED = "PaymentConfirmed";
    private static final String PAYMENT_FAILED = "PaymentFailed";
    private static final String VERSION = "1.0";

    private static final Logger log = LoggerFactory.getLogger(PaymentEventPublisherKafka.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String paymentConfirmedTopic;
    private final String paymentFailedTopic;

    PaymentEventPublisherKafka(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               KafkaTopicsConfig topicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.paymentConfirmedTopic = topicsConfig.getPaymentConfirmed();
        this.paymentFailedTopic = topicsConfig.getPaymentFailed();
    }

    @Override
    public void publishPaymentConfirmed(PaymentConfirmedEvent event) {
        Map<String, Object> envelope = new HashMap<>(envelope(event.eventId(), PAYMENT_CONFIRMED, event.correlationId()));
        envelope.put("payload", Map.of(
                "paymentId", event.paymentId(),
                "orderId", event.orderId(),
                "amount", event.amount(),
                "currency", event.currency(),
                "status", event.status()
        ));
        send(paymentConfirmedTopic, event.orderId(), envelope);
    }

    @Override
    public void publishPaymentFailed(PaymentFailedEvent event) {
        Map<String, Object> envelope = new HashMap<>(envelope(event.eventId(), PAYMENT_FAILED, event.correlationId()));
        envelope.put("payload", Map.of(
                "paymentId", event.paymentId(),
                "orderId", event.orderId(),
                "amount", event.amount(),
                "currency", event.currency(),
                "status", event.status(),
                "reason", event.reason()
        ));
        send(paymentFailedTopic, event.orderId(), envelope);
    }

    private Map<String, Object> envelope(String eventId, String eventType, String correlationId) {
        return Map.of(
                "eventId", eventId,
                "eventType", eventType,
                "version", VERSION,
                "correlationId", correlationId != null ? correlationId : "",
                "timestamp", Instant.now().toString()
        );
    }

    private void send(String topic, String key, Map<String, Object> envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, json);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish to {} key={} eventId={}", topic, key, envelope.get("eventId"), ex);
                } else {
                    log.debug("Published to {} key={}", topic, key);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Serialization error publishing to {} key={}", topic, key, e);
            throw new RuntimeException(e);
        }
    }
}
