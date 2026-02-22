package com.example.orderservice.application.port;

/**
 * Port for idempotency: record that an event (by eventId) was already processed.
 * Duplicate deliveries should be ignored when eventId is already present.
 */
public interface ProcessedEventStore {

    boolean alreadyProcessed(String eventId);

    void markProcessed(String eventId);
}
