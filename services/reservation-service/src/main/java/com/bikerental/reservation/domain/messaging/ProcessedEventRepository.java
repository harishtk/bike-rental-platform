package com.bikerental.reservation.domain.messaging;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedEventRepository {

    boolean exists(
            UUID eventId,
            String consumerName
    );

    void markProcessed(
            UUID eventId,
            String consumerName,
            Instant processedAt
    );
}
