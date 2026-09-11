package com.bikerental.reservation.infrastructure.persistence.messaging;

import com.bikerental.reservation.domain.messaging.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProcessedEventRepositoryAdapter
    implements ProcessedEventRepository {

    private final SpringDataProcessedEventRepository repository;

    @Override
    public boolean exists(UUID eventId, String consumerName) {
        return repository.existsByIdEventIdAndIdConsumerName(eventId, consumerName);
    }

    @Override
    public void markProcessed(UUID eventId, String consumerName, Instant processedAt) {
        repository.save(
                new ProcessedEventEntity(
                        new ProcessedEventId(eventId, consumerName),
                        processedAt
                )
        );
    }
}
