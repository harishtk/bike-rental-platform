package com.bikerental.reservation.application.reservation.event;

import com.bikerental.reservation.application.messaging.EventEnvelope;
import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.reservation.Reservation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReservationOutboxEventFactory {

    private final EventPayloadSerializer serializer;

    public OutboxEvent created(Reservation reservation) {
        ReservationCreatedEvent event =
                new ReservationCreatedEvent(
                        reservation.getId(),
                        reservation.getUserId(),
                        reservation.getBikeId(),
                        reservation.getStationId(),
                        reservation.getReservedAt(),
                        reservation.getExpiresAt()
                );

        return create(
                reservation,
                ReservationEventTypes.CREATED,
                reservation.getReservedAt(),
                event
        );
    }

    public OutboxEvent cancelled(Reservation reservation) {
        ReservationCancelledEvent event =
                new ReservationCancelledEvent(
                        reservation.getId(),
                        reservation.getUserId(),
                        reservation.getBikeId(),
                        reservation.getStationId(),
                        reservation.getCancelledAt()
                );

        return create(
                reservation,
                ReservationEventTypes.CANCELLED,
                reservation.getCancelledAt(),
                event
        );
    }

    public OutboxEvent expired(Reservation reservation, Instant expiredAt) {
        ReservationExpiredEvent event =
                new ReservationExpiredEvent(
                        reservation.getId(),
                        reservation.getUserId(),
                        reservation.getBikeId(),
                        reservation.getStationId(),
                        expiredAt
                );

        return create(
                reservation,
                ReservationEventTypes.EXPIRED,
                event.expiredAt(),
                event
        );
    }

    private OutboxEvent create(Reservation reservation,
                               String eventType,
                               Instant occurredAt,
                               Object payload) {
        UUID eventId = UUID.randomUUID();

        EventEnvelope<Object> envelope =
                new EventEnvelope<>(
                        eventId,
                        eventType,
                        ReservationEventTypes.AGGREGATE_TYPE,
                        reservation.getId(),
                        occurredAt,
                        payload
                );

        String serializedPayload =  serializer.serialize(envelope);

        return OutboxEvent.create(
                eventId,
                ReservationEventTypes.AGGREGATE_TYPE,
                reservation.getId(),
                eventType,
                serializedPayload,
                occurredAt
        );
    }
}
