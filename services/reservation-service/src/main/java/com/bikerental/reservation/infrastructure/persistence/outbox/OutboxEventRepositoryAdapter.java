package com.bikerental.reservation.infrastructure.persistence.outbox;

import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Repository
public class OutboxEventRepositoryAdapter
    implements OutboxEventRepository {

    private final SpringDataOutboxEventRepository repository;
    private final OutboxEventEntityMapper mapper;

    @Override
    public OutboxEvent save(OutboxEvent event) {
        return mapper.toDomain(
                repository.save(
                        mapper.toEntity(event)
                )
        );
    }

    @Override
    public List<OutboxEvent> findUnpublishedEvents(int limit) {
        return repository
                .findByPublishedAtIsNullOrderByOccurredAtAsc(
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void markPublished(UUID eventId, Instant publishedAt) {
        repository.markPublished(eventId, publishedAt);
    }
}
