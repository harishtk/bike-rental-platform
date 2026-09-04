package com.bikerental.reservation.api.reservation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record CreateReservationRequest(

        @NotNull
        UUID userId,

        @NotNull
        UUID bikeId,

        @Positive
        long durationHours
) {
}