package com.bikerental.reservation.application.outbox;

import com.bikerental.reservation.application.reservation.event.ReservationEventTypes;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import com.bikerental.reservation.infrastructure.messaging.kafka.KafkaEventPublicationException;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
class OutboxPublisherServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private OutboxEventPublisher outboxEventPublisher;
    @Mock
    private OutboxPublicationService outboxPublicationService;
    @InjectMocks
    private OutboxPublisherService outboxPublisherService;

    @Test
    void givenUnpublishedEventWhenPublisherRunsThenEventIsPublished() {
        OutboxEvent outboxEvent = OutboxEvent.create(
                UUID.randomUUID(),
                ReservationEventTypes.AGGREGATE_TYPE,
                UUID.randomUUID(),
                ReservationEventTypes.CREATED,
                "{\"data\":\"test\"}",
                Instant.now()
        );

        when(outboxEventRepository.findUnpublishedEvents(anyInt()))
                .thenReturn(List.of(outboxEvent));

        outboxPublisherService.publishEvents();

        verify(outboxEventPublisher).publish(outboxEvent);
        verify(outboxPublicationService).markPublished(outboxEvent.getId());
    }

    @Test
    void givenKafkaFailsWhenPublishCalledThenNeverMarkedPublished() {
        OutboxEvent outboxEvent = OutboxEvent.create(
                UUID.randomUUID(),
                ReservationEventTypes.AGGREGATE_TYPE,
                UUID.randomUUID(),
                ReservationEventTypes.CREATED,
                "{\"data\":\"test\"}",
                Instant.now()
        );
        when(outboxEventRepository.findUnpublishedEvents(anyInt()))
                .thenReturn(List.of(outboxEvent));
        doThrow(new KafkaEventPublicationException("Failed to publish events", new RuntimeException()))
                .when(outboxEventPublisher)
                .publish(outboxEvent);

        outboxPublisherService.publishEvents();

        verify(outboxEventPublisher).publish(any(OutboxEvent.class));
        verify(outboxPublicationService, never())
                .markPublished(outboxEvent.getId());

        assertThat(outboxEvent.getPublishedAt()).isNull();
    }

    @Test
    void givenOneFailedPublishWhenPublishedThenBatchContinues() {
        OutboxEvent eventA = OutboxEvent.create(
                UUID.randomUUID(),
                ReservationEventTypes.AGGREGATE_TYPE,
                UUID.randomUUID(),
                ReservationEventTypes.CREATED,
                "{\"data\":\"test\"}",
                Instant.now().minus(Duration.ofDays(2))
        );

        OutboxEvent eventB = OutboxEvent.create(
                UUID.randomUUID(),
                ReservationEventTypes.AGGREGATE_TYPE,
                UUID.randomUUID(),
                ReservationEventTypes.CREATED,
                "{\"data\":\"test\"}",
                Instant.now().minus(Duration.ofDays(1))
        );

        OutboxEvent eventC = OutboxEvent.create(
                UUID.randomUUID(),
                ReservationEventTypes.AGGREGATE_TYPE,
                UUID.randomUUID(),
                ReservationEventTypes.CREATED,
                "{\"data\":\"test\"}",
                Instant.now().minus(Duration.ofDays(3))
        );

        when(outboxEventRepository.findUnpublishedEvents(anyInt()))
                .thenReturn(List.of(eventA, eventB, eventC));
        doThrow(new KafkaEventPublicationException("Failed to publish events", new RuntimeException()))
                .when(outboxEventPublisher)
                .publish(eventB);

        outboxPublisherService.publishEvents();

        verify(outboxPublicationService).markPublished(eventA.getId());
        verify(outboxPublicationService).markPublished(eventC.getId());
        verify(outboxPublicationService, never()).markPublished(eventB.getId());
    }
}