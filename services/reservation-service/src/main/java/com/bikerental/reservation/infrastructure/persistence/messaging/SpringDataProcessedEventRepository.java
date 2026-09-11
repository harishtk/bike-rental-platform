package com.bikerental.reservation.infrastructure.persistence.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataProcessedEventRepository
    extends JpaRepository<ProcessedEventEntity, ProcessedEventId> {

    boolean existsByIdEventIdAndIdConsumerName(
            UUID eventId,
            String consumerName
    );
}
