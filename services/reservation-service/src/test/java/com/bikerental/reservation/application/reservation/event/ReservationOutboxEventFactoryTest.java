package com.bikerental.reservation.application.reservation.event;

import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.bikerental.reservation.application.messaging.EventEnvelope;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.reservation.Reservation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationOutboxEventFactoryTest {

    @Mock
    private EventPayloadSerializer serializer;

    @Test
    void shouldCreateReservationCreatedOutboxEventWithEnvelope() {
        ReservationOutboxEventFactory factory =
                new ReservationOutboxEventFactory(serializer);

        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        Reservation reservation =
                Reservation.create(
                        userId,
                        bikeId,
                        stationId,
                        Duration.ofHours(24)
                );

        when(serializer.serialize(any()))
                .thenReturn("{\"event\":\"serialized\"}");

        OutboxEvent outboxEvent =
                factory.created(reservation);

        ArgumentCaptor<Object> envelopeCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(serializer)
                .serialize(envelopeCaptor.capture());

        Object serializedObject =
                envelopeCaptor.getValue();

        assertThat(serializedObject)
                .isInstanceOf(EventEnvelope.class);

        EventEnvelope<?> envelope =
                (EventEnvelope<?>) serializedObject;

        assertThat(envelope.eventId())
                .isEqualTo(outboxEvent.getId());

        assertThat(envelope.eventType())
                .isEqualTo(ReservationEventTypes.CREATED);

        assertThat(envelope.aggregateType())
                .isEqualTo(
                        ReservationEventTypes.AGGREGATE_TYPE
                );

        assertThat(envelope.aggregateId())
                .isEqualTo(reservation.getId());

        assertThat(envelope.payload())
                .isInstanceOf(
                        ReservationCreatedEvent.class
                );

        ReservationCreatedEvent payload =
                (ReservationCreatedEvent)
                        envelope.payload();

        assertThat(payload.reservationId())
                .isEqualTo(reservation.getId());

        assertThat(payload.userId())
                .isEqualTo(userId);

        assertThat(payload.bikeId())
                .isEqualTo(bikeId);

        assertThat(payload.stationId())
                .isEqualTo(stationId);

        assertThat(outboxEvent.getAggregateType())
                .isEqualTo(
                        ReservationEventTypes.AGGREGATE_TYPE
                );

        assertThat(outboxEvent.getAggregateId())
                .isEqualTo(reservation.getId());

        assertThat(outboxEvent.getEventType())
                .isEqualTo(
                        ReservationEventTypes.CREATED
                );

        assertThat(outboxEvent.getPayload())
                .isEqualTo(
                        "{\"event\":\"serialized\"}"
                );
    }
}