package com.bikerental.reservation.infrastructure.client.bike;

import java.util.UUID;

public record BikeFeignResponse(

        UUID id,

        UUID stationId
) {
}