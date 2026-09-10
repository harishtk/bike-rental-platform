package com.bikerental.reservation.domain.outbox;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class OutboxEvent {

    private final UUID id;
    private final String aggregateType;
    private final UUID aggregateId;
    private final String eventType;
    private final String payload;
    private final Instant occurredAt;
    private Instant publishedAt;

    public static OutboxEvent create(
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String payload,
            Instant occurredAt
    ) {
        return new OutboxEvent(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventType,
                payload,
                occurredAt
        );
    }

    public void markPublished(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
