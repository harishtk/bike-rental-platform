package com.bikerental.bike.api.station;

import com.bikerental.bike.domain.station.StationStatus;

import java.time.Instant;
import java.util.UUID;

public record StationResponse(
        UUID id,
        String name,
        String address,
        int capacity,
        StationStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}