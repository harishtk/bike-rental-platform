package com.bikerental.reservation.application.bike;

import java.util.UUID;

public record BikeReservationDetails(
        UUID bikeId,
        UUID stationId
) {
}