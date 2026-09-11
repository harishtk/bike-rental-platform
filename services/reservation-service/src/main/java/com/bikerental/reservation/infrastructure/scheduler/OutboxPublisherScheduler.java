package com.bikerental.reservation.infrastructure.scheduler;

import com.bikerental.reservation.application.outbox.OutboxPublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxPublisherService outboxPublisherService;

    @Scheduled(
            fixedDelayString = "${outbox.publisher.fixed-delay:5000}"
    )
    public void publishOutboxEvents() {
        outboxPublisherService.publishEvents();
    }
}
