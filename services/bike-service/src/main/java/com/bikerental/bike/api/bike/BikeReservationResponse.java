package com.bikerental.bike.api.bike;

import java.util.UUID;

public record BikeReservationResponse(
        UUID id,
        UUID stationId
) {
}