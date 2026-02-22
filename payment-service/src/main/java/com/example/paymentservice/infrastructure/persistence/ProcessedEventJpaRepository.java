package com.example.paymentservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProcessedEventJpaRepository extends JpaRepository<ProcessedEventEntity, Long> {

    boolean existsByEventId(String eventId);
}
