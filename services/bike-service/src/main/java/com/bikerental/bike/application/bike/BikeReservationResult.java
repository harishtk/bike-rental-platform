package com.bikerental.bike.application.bike;

import java.util.UUID;

public record BikeReservationResult(
        UUID id,
        UUID stationId
) {
}