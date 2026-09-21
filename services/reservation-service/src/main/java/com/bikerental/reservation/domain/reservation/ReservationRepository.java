package com.bikerental.reservation.domain.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    void flush();

    Optional<Reservation> findByIdAndUserId(UUID reservationId, UUID userId);

    boolean existsByUserIdAndStatus(
            UUID userId,
            ReservationStatus status
    );

    List<Reservation> findExpiredActiveReservations(
            Instant currentTime
    );

    List<Reservation> findByUserId(UUID userId);
}
