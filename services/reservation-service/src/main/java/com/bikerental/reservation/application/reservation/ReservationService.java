package com.bikerental.reservation.application.reservation;

import java.time.Clock;
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
import com.bikerental.reservation.application.reservation.event.ReservationOutboxEventFactory;
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
    private final ReservationOutboxEventFactory reservationOutboxEventFactory;

    private final Clock clock;

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

        UUID reserveOperationId = UUID.randomUUID();

        BikeReservationDetails bikeDetails =
                bikeReservationGateway.reserveBike(
                        bikeId,
                        reserveOperationId
                );

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

            OutboxEvent outboxEvent =
                    reservationOutboxEventFactory.created(reservation);

            outboxEventRepository.save(outboxEvent);

            return savedReservation;
        } catch (RuntimeException e) {
            bikeReservationGateway.releaseBike(bikeId, reserveOperationId);
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

        UUID releaseOperationId = UUID.randomUUID();

        bikeReservationGateway.releaseBike(
                reservation.getBikeId(),
                releaseOperationId
        );

        reservation.cancel(Instant.now(clock));

        Reservation savedReservation = reservationRepository.save(reservation);

        OutboxEvent event =
                reservationOutboxEventFactory.cancelled(savedReservation);
        outboxEventRepository.save(event);

        return savedReservation;
    }
}