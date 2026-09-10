package com.bikerental.reservation.application.reservation.event;

import java.time.Instant;
import java.util.UUID;

public record ReservationCreatedEvent(
        UUID reservationId,
        UUID userId,
        UUID bikeId,
        UUID stationId,
        Instant reservedAt,
        Instant expiresAt
) {
}
