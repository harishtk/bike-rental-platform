package com.bikerental.reservation.domain.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository {

    OutboxEvent save(OutboxEvent event);

    List<OutboxEvent> findUnpublishedEvents(int limit);

    void markPublished(UUID eventId, Instant publishedAt);
}
