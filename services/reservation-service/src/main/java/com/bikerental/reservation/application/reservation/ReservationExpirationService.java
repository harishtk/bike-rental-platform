package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.application.reservation.event.ReservationOutboxEventFactory;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Service
public class ReservationExpirationService {

    private final ReservationRepository reservationRepository;
    private final BikeReservationGateway bikeReservationGateway;
    private final Clock clock;

    private final OutboxEventRepository outboxEventRepository;
    private final ReservationOutboxEventFactory  reservationOutboxEventFactory;

    @Transactional
    public void expireReservations() {
        Instant now = Instant.now(clock);

        List<Reservation> expiredReservations = reservationRepository
                .findExpiredActiveReservations(now);

        for (Reservation reservation : expiredReservations) {
            try {
                expireReservation(reservation, now);
            } catch (RuntimeException e) {
                log.error("e: ", e);
            }
        }
    }

    private void expireReservation(Reservation reservation, Instant currentTime) {
        UUID expireOperationId = UUID.randomUUID();
        bikeReservationGateway.releaseBike(reservation.getBikeId(), expireOperationId);

        reservation.expire(currentTime);

        Reservation saved = reservationRepository.save(reservation);

        OutboxEvent event =
                reservationOutboxEventFactory.expired(saved, currentTime);
        outboxEventRepository.save(event);
    }
}
