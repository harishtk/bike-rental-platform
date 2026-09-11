package com.bikerental.bike.application.idempotency;

import java.time.Instant;
import java.util.UUID;

public record BikeOperation(
        UUID operationId,
        UUID bikeId,
        BikeOperationType operationType,
        UUID resultStationId,
        Instant processedAt
) {

    public static BikeOperation reserve(
            UUID operationId,
            UUID bikeId,
            UUID stationId,
            Instant processedAt
    ) {
        return new BikeOperation(
                operationId,
                bikeId,
                BikeOperationType.RESERVE,
                stationId,
                processedAt
        );
    }

    public static BikeOperation release(
            UUID operationId,
            UUID bikeId,
            Instant processedAt
    ) {
        return new BikeOperation(
                operationId,
                bikeId,
                BikeOperationType.RELEASE,
                null,
                processedAt
        );
    }
}