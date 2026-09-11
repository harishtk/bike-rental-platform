package com.bikerental.reservation.application.outbox;

import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxPublicationService {

    private final OutboxEventRepository outboxEventRepository;
    private final Clock clock;

    @Transactional
    public void markPublished(UUID eventId) {
        outboxEventRepository.markPublished(eventId, Instant.now(clock));
    }
}
