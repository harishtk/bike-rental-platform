package com.bikerental.reservation.domain.reservation;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
public class Reservation {

    private static final Duration MAX_RESERVATION_DURATION =
            Duration.ofDays(7);

    private final UUID id;
    private final UUID userId;
    private final UUID bikeId;
    private final UUID stationId;
    private final Instant reservedAt;
    private final Instant expiresAt;
    private ReservationStatus status;
    private Instant cancelledAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static Reservation create(
            UUID userId,
            UUID bikeId,
            UUID stationId,
            Duration duration
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (bikeId == null) {
            throw new IllegalArgumentException("Bike ID is required");
        }

        if (stationId == null) {
            throw new IllegalArgumentException("Station ID is required");
        }

        validateDuration(duration);

        Instant now = Instant.now();

        return new Reservation(
                UUID.randomUUID(),
                userId,
                bikeId,
                stationId,
                now,
                now.plus(duration),
                ReservationStatus.ACTIVE,
                null,
                now,
                now
        );
    }

    public static Reservation restore(
            UUID id,
            UUID userId,
            UUID bikeId,
            UUID stationId,
            Instant reservedAt,
            Instant expiresAt,
            ReservationStatus status,
            Instant cancelledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Reservation(
                id,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                status,
                cancelledAt,
                createdAt,
                updatedAt
        );
    }

    public void cancel(Instant currentTime) {
        ensureActive();

        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = currentTime;
        touch();
    }

    public void expire(Instant currentTime) {
        ensureActive();

        if (currentTime.isBefore(expiresAt)) {
            throw new InvalidReservationStateException(
                    "Reservation has not expired yet"
            );
        }

        this.status = ReservationStatus.EXPIRED;
        this.updatedAt = currentTime;
    }

    public void consume(Instant currentTime) {
        ensureActive();

        if (currentTime.isAfter(expiresAt)) {
            throw new InvalidReservationStateException(
                    "Reservation has already expired"
            );
        }

        this.status = ReservationStatus.CONSUMED;
        this.updatedAt = currentTime;
    }

    public boolean isExpired() {
        return status == ReservationStatus.ACTIVE
                && !Instant.now().isBefore(expiresAt);
    }

    private static void validateDuration(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(
                    "Reservation duration must be greater than zero"
            );
        }

        if (duration.compareTo(MAX_RESERVATION_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "Reservation duration cannot exceed 7 days"
            );
        }
    }

    private void ensureActive() {
        if (status != ReservationStatus.ACTIVE) {
            throw new InvalidReservationStateException(
                    "Reservation is not active. Current status: " + status
            );
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
