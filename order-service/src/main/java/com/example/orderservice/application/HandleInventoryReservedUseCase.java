package com.example.orderservice.application;

import com.example.orderservice.application.port.OrderEventPublisher;
import com.example.orderservice.application.port.OrderRepository;
import com.example.orderservice.application.port.ProcessedEventStore;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Handles InventoryReserved: confirms the order and publishes OrderConfirmed.
 * Idempotent by eventId; duplicate events are no-op.
 */
public class HandleInventoryReservedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleInventoryReservedUseCase.class);

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final ProcessedEventStore processedEventStore;

    public HandleInventoryReservedUseCase(OrderRepository orderRepository,
                                         OrderEventPublisher eventPublisher,
                                         ProcessedEventStore processedEventStore) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.processedEventStore = processedEventStore;
    }

    /**
     * @return true if the event was applied (order confirmed); false if duplicate or order not in PENDING
     */
    public boolean execute(String eventId, String correlationId, String orderId) {
        if (processedEventStore.alreadyProcessed(eventId)) {
            log.debug("Duplicate InventoryReserved event ignored, eventId={}", eventId);
            return false;
        }
        UUID id = UUID.fromString(orderId);
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            log.warn("Order not found for InventoryReserved, orderId={}", orderId);
            return false;
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            log.debug("Order not PENDING, skipping InventoryReserved, orderId={} status={}", orderId, order.getStatus());
            return false;
        }
        order.confirm();
        orderRepository.save(order);
        processedEventStore.markProcessed(eventId);

        String confirmedEventId = UUID.randomUUID().toString();
        eventPublisher.publishOrderConfirmed(new OrderEventPublisher.OrderConfirmedEvent(
                confirmedEventId,
                correlationId,
                orderId,
                order.getCustomerId(),
                OrderStatus.CONFIRMED.name(),
                Instant.now().toString()
        ));
        log.info("Order confirmed, orderId={} correlationId={}", orderId, correlationId);
        return true;
    }
}
