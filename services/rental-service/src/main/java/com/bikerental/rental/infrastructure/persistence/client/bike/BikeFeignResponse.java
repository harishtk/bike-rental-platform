package com.bikerental.rental.infrastructure.persistence.client.bike;

import java.util.UUID;

public record BikeFeignResponse(
        UUID id,
        UUID stationId
) {
}