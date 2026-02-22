package com.example.paymentservice.infrastructure.persistence;

import com.example.paymentservice.application.port.ProcessedEventStore;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class JpaProcessedEventStore implements ProcessedEventStore {

    private final ProcessedEventJpaRepository repository;

    JpaProcessedEventStore(ProcessedEventJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean alreadyProcessed(String eventId) {
        return repository.existsByEventId(eventId);
    }

    @Override
    public void markProcessed(String eventId) {
        repository.save(new ProcessedEventEntity(eventId, Instant.now()));
    }
}
