package com.bikerental.rental.api.rental;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ReturnRentalRequest(
        @NotNull UUID stationId
) {
}