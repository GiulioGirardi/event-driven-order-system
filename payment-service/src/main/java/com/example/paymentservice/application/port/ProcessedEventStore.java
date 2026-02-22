package com.example.paymentservice.application.port;

/**
 * Idempotency: record processed eventIds so duplicate deliveries are ignored.
 */
public interface ProcessedEventStore {

    boolean alreadyProcessed(String eventId);

    void markProcessed(String eventId);
}
