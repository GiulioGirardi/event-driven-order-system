package com.example.orderservice.application;

import com.example.orderservice.application.port.OrderRepository;
import com.example.orderservice.application.port.ProcessedEventStore;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles InventoryFailed: marks order as FAILED. Idempotent by eventId.
 * (Refund is handled by payment-service via payment.refund.requested.)
 */
public class HandleInventoryFailedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleInventoryFailedUseCase.class);

    private final OrderRepository orderRepository;
    private final ProcessedEventStore processedEventStore;

    public HandleInventoryFailedUseCase(OrderRepository orderRepository,
                                        ProcessedEventStore processedEventStore) {
        this.orderRepository = orderRepository;
        this.processedEventStore = processedEventStore;
    }

    public boolean execute(String eventId, String orderId) {
        if (processedEventStore.alreadyProcessed(eventId)) {
            log.debug("Duplicate InventoryFailed event ignored, eventId={}", eventId);
            return false;
        }
        Order order = orderRepository.findById(UUID.fromString(orderId)).orElse(null);
        if (order == null) {
            log.warn("Order not found for InventoryFailed, orderId={}", orderId);
            return false;
        }
        order.fail();
        orderRepository.save(order);
        processedEventStore.markProcessed(eventId);
        log.info("Order marked FAILED (inventory), orderId={}", orderId);
        return true;
    }
}
