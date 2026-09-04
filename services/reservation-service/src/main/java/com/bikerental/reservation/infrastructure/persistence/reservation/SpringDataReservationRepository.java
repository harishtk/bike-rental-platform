package com.bikerental.reservation.infrastructure.persistence.reservation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bikerental.reservation.domain.reservation.ReservationStatus;

public interface SpringDataReservationRepository
        extends JpaRepository<ReservationEntity, UUID> {

    boolean existsByUserIdAndStatus(
            UUID userId,
            ReservationStatus status
    );

    List<ReservationEntity>
    findByStatusAndExpiresAtLessThanEqual(
            ReservationStatus status,
            Instant expiresAt
    );
}