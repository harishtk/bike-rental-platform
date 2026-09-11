package com.bikerental.reservation.application.outbox;

import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisherService {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventPublisher outboxEventPublisher;
    private final OutboxPublicationService outboxPublicationService;

    public void publishEvents() {
        List<OutboxEvent> events =
                outboxEventRepository.findUnpublishedEvents(BATCH_SIZE);

        for (OutboxEvent event : events) {
            try {
                outboxEventPublisher.publish(event);
                outboxPublicationService.markPublished(event.getId());
            } catch (Exception exception) {
                log.error("Failed to publish reservation event for reservation {}", event.getAggregateId(), exception);
            }
        }
    }
}
