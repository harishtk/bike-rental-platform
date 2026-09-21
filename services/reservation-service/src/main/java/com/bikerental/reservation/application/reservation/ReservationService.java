package com.bikerental.reservation.application.reservation;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
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
    public Reservation getReservation(UUID reservationId, UUID userId) {

        return reservationRepository
                .findByIdAndUserId(reservationId, userId)
                .orElseThrow(() ->
                        new ReservationNotFoundException(reservationId)
                );
    }

    @Transactional(readOnly = true)
    public List<Reservation> getReservations(UUID userId) {
        return reservationRepository.findByUserId(userId);
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId, UUID userId) {

        Reservation reservation = getReservation(reservationId, userId);

        reservation.cancel(Instant.now(clock));

        UUID releaseOperationId = UUID.randomUUID();

        bikeReservationGateway.releaseBike(
                reservation.getBikeId(),
                releaseOperationId
        );

        Reservation savedReservation = reservationRepository.save(reservation);

        OutboxEvent event =
                reservationOutboxEventFactory.cancelled(savedReservation);
        outboxEventRepository.save(event);

        return savedReservation;
    }
}