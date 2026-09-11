package com.bikerental.rental.api.rental;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateRentalRequest(
        @NotNull UUID userId,
        @NotNull UUID bikeId,
        @NotNull BigDecimal dailyRate
) {
}