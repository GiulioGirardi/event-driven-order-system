package com.example.orderservice.infrastructure.kafka;

import com.example.orderservice.application.port.OrderEventPublisher;
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

/**
 * Publishes order events to Kafka using the envelope + payload structure.
 * Key is orderId for partition ordering.
 */
@Component
class OrderEventPublisherKafka implements OrderEventPublisher {

    private static final String ORDER_CREATED = "OrderCreated";
    private static final String ORDER_CONFIRMED = "OrderConfirmed";
    private static final String VERSION = "1.0";

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisherKafka.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String orderCreatedTopic;
    private final String orderConfirmedTopic;

    OrderEventPublisherKafka(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper,
                             KafkaTopicsConfig topicsConfig) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.orderCreatedTopic = topicsConfig.getOrderCreated();
        this.orderConfirmedTopic = topicsConfig.getOrderConfirmed();
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        Map<String, Object> envelope = new HashMap<>(envelope(event.eventId(), ORDER_CREATED, event.correlationId()));
        envelope.put("payload", Map.of(
                "orderId", event.orderId(),
                "customerId", event.customerId(),
                "totalAmount", event.totalAmount(),
                "currency", event.currency(),
                "items", event.items().stream()
                        .map(i -> Map.of("productId", i.productId(), "quantity", i.quantity(), "unitPrice", i.unitPrice()))
                        .toList()
        ));
        send(orderCreatedTopic, event.orderId(), envelope);
    }

    @Override
    public void publishOrderConfirmed(OrderConfirmedEvent event) {
        Map<String, Object> envelope = new HashMap<>(envelope(event.eventId(), ORDER_CONFIRMED, event.correlationId()));
        envelope.put("payload", Map.of(
                "orderId", event.orderId(),
                "customerId", event.customerId(),
                "status", event.status(),
                "confirmedAt", event.confirmedAt()
        ));
        send(orderConfirmedTopic, event.orderId(), envelope);
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
                    log.debug("Published to {} key={} partition={}", topic, key,
                            result != null ? result.getRecordMetadata().partition() : null);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Serialization error publishing to {} key={}", topic, key, e);
            throw new RuntimeException(e);
        }
    }
}
