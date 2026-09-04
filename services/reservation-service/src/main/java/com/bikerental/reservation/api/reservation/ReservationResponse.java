package com.bikerental.reservation.api.reservation;

import java.time.Instant;
import java.util.UUID;

import com.bikerental.reservation.domain.reservation.ReservationStatus;

public record ReservationResponse(

        UUID id,

        UUID userId,

        UUID bikeId,

        UUID stationId,

        ReservationStatus status,

        Instant reservedAt,

        Instant expiresAt,

        Instant cancelledAt
) {
}