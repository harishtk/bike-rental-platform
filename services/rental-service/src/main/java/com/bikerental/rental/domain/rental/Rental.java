package com.bikerental.rental.domain.rental;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class Rental {

    private final UUID id;
    private final UUID userId;
    private final UUID bikeId;

    private UUID startStationId;
    private UUID returnStationId;

    private final Instant startedAt;
    private Instant returnedAt;

    private RentalStatus status;

    private final BigDecimal dailyRate;
    private BigDecimal totalAmount;

    private final Instant createdAt;
    private Instant updatedAt;

    public static Rental start(
            UUID userId,
            UUID bikeId,
            UUID stationId,
            BigDecimal dailyRate,
            Instant now
    ) {
        return new Rental(
                UUID.randomUUID(),
                userId,
                bikeId,
                stationId,
                null,
                now,
                null,
                RentalStatus.ACTIVE,
                dailyRate,
                BigDecimal.ZERO,
                now,
                now
        );
    }

    public static Rental restore(
            UUID id,
            UUID userId,
            UUID bikeId,
            UUID startStationId,
            UUID returnStationId,
            Instant startedAt,
            Instant returnedAt,
            RentalStatus status,
            BigDecimal dailyRate,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new Rental(
                id,
                userId,
                bikeId,
                startStationId,
                returnStationId,
                startedAt,
                returnedAt,
                status,
                dailyRate,
                totalAmount,
                createdAt,
                updatedAt
        );
    }

    public void returnBike(
            UUID stationId,
            Instant now,
            BigDecimal totalAmount
    ) {
        if (status != RentalStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Only active rental can be returned"
            );
        }

        this.returnStationId = stationId;
        this.returnedAt = now;
        this.totalAmount = totalAmount;
        this.status = RentalStatus.RETURNED;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        if (status != RentalStatus.RETURNED) {
            throw new IllegalStateException(
                    "Rental must be returned before completion"
            );
        }

        status = RentalStatus.COMPLETED;
        updatedAt = now;
    }

}