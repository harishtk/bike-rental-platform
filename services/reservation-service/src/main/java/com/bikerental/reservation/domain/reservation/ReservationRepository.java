package com.bikerental.reservation.domain.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    void flush();

    Optional<Reservation> findById(UUID reservationId);

    boolean existsByUserIdAndStatus(
            UUID userId,
            ReservationStatus status
    );

    List<Reservation> findExpiredActiveReservations(
            Instant currentTime
    );

    List<Reservation> allReservations();
}
