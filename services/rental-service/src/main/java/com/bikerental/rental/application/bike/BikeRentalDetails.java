package com.bikerental.rental.application.bike;

import java.util.UUID;

public record BikeRentalDetails(
        UUID bikeId,
        UUID stationId
) {
}