package com.bikerental.bike.api.bike;

import com.bikerental.bike.domain.bike.BikeStatus;

import java.time.Instant;
import java.util.UUID;

public record BikeResponse(
        UUID id,
        String serialNumber,
        String type,
        BikeStatus status,
        UUID stationId,
        Instant createdAt,
        Instant updatedAt
) {
}
