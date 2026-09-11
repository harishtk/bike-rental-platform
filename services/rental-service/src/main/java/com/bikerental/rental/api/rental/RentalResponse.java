package com.bikerental.rental.api.rental;

import com.bikerental.rental.domain.rental.RentalStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RentalResponse(
        UUID id,
        UUID userId,
        UUID bikeId,
        UUID startStationId,
        UUID returnStationId,
        RentalStatus status,
        Instant startedAt,
        Instant returnedAt,
        BigDecimal dailyRate,
        BigDecimal totalAmount
) {
}