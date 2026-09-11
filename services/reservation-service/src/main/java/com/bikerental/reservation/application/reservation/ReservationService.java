package com.bikerental.reservation.application.reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.application.messaging.EventEnvelope;
import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.bikerental.reservation.application.reservation.event.ReservationCreatedEvent;
import com.bikerental.reservation.application.reservation.event.ReservationEventTypes;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;

@RequiredArgsConstructor
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BikeReservationGateway bikeReservationGateway;

    private final OutboxEventRepository outboxEventRepository;
    private final EventPayloadSerializer eventPayloadSerializer;

    @Transactional
    public Reservation createReservation(
            UUID userId,
            UUID bikeId,
            Duration duration
    ) {
        boolean hasActiveReservation =
                reservationRepository.existsByUserIdAndStatus(
                        userId,
                        ReservationStatus.ACTIVE
                );

        if (hasActiveReservation) {
            throw new ActiveReservationAlreadyExistsException(userId);
        }

        BikeReservationDetails bikeDetails =
                bikeReservationGateway.reserveBike(bikeId);

        try {
            Reservation reservation =
                    Reservation.create(
                            userId,
                            bikeId,
                            bikeDetails.stationId(),
                            duration
                    );

            Reservation savedReservation = reservationRepository.save(reservation);
            reservationRepository.flush();

            UUID eventId = UUID.randomUUID();

            ReservationCreatedEvent event =
                    new ReservationCreatedEvent(
                            savedReservation.getId(),
                            savedReservation.getUserId(),
                            savedReservation.getBikeId(),
                            savedReservation.getStationId(),
                            savedReservation.getReservedAt(),
                            savedReservation.getExpiresAt()
                    );

            EventEnvelope<ReservationCreatedEvent> envelope =
                    new EventEnvelope<>(
                            eventId,
                            ReservationEventTypes.CREATED,
                            ReservationEventTypes.AGGREGATE_TYPE,
                            savedReservation.getId(),
                            savedReservation.getReservedAt(),
                            event
                    );

            String payload = eventPayloadSerializer.serialize(envelope);

            OutboxEvent outboxEvent =
                    OutboxEvent.create(
                            eventId,
                            ReservationEventTypes.AGGREGATE_TYPE,
                            savedReservation.getId(),
                            ReservationEventTypes.CREATED,
                            payload,
                            savedReservation.getReservedAt()
                    );
            outboxEventRepository.save(outboxEvent);

            return savedReservation;
        } catch (RuntimeException e) {
            bikeReservationGateway.releaseBike(bikeId);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(UUID reservationId) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ReservationNotFoundException(reservationId)
                );
    }

    @Transactional(readOnly = true)
    public List<Reservation> allReservations() {
        return reservationRepository.allReservations();
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId) {

        Reservation reservation = getReservation(reservationId);

        bikeReservationGateway.releaseBike(reservation.getBikeId());

        reservation.cancel(Instant.now());

        return reservationRepository.save(reservation);
    }
}