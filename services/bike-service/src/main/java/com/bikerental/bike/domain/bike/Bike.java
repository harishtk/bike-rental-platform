package com.bikerental.bike.domain.bike;

import java.time.Instant;
import java.util.UUID;

public class Bike {

    private final UUID id;
    private final String serialNumber;
    private final String type;

    private BikeStatus status;
    private UUID stationId;

    private final Instant createdAt;
    private Instant updatedAt;

    private long version;

    public Bike(UUID id,
                String serialNumber,
                String type,
                BikeStatus status,
                UUID stationId,
                Instant createdAt,
                Instant updatedAt, long version) {
        this.id = id;
        this.serialNumber = serialNumber;
        this.type = type;
        this.status = status;
        this.stationId = stationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;

        validate();
    }

    public static Bike create(
            String serialNumber,
            String type,
            UUID stationId
    ) {
        Instant now = Instant.now();

        return new Bike(
                UUID.randomUUID(),
                serialNumber,
                type,
                BikeStatus.AVAILABLE,
                stationId,
                now,
                now,
                0
        );
    }

    public void reserve() {
        ensureStatus(BikeStatus.AVAILABLE);
        status = BikeStatus.RESERVED;
        touch();
    }

    public void release() {
        ensureStatus(BikeStatus.RESERVED);
        status = BikeStatus.AVAILABLE;
        touch();
    }

    public void rent() {
        if (status != BikeStatus.AVAILABLE &&
            status != BikeStatus.RESERVED) {
            throw new InvalidBikeStateException(
                    "Bike cannot be rented from status " + status
            );
        }

        status = BikeStatus.RENTED;
        touch();
    }

    public void returnToStation(UUID returnStationId) {
        if (status != BikeStatus.RENTED) {
            throw new InvalidBikeStateException(
                    "Bike cannot be returned from status " + status
            );
        }

        if (returnStationId == null) {
            throw new IllegalArgumentException(
                    "Return station cannot be null"
            );
        }

        stationId = returnStationId;
        status = BikeStatus.AVAILABLE;
        touch();
    }

    public void startMaintenance() {
        if (status != BikeStatus.AVAILABLE) {
            throw new InvalidBikeStateException(
                    "Bike cannot enter maintenance from status " + status
            );
        }

        status = BikeStatus.MAINTENANCE;
        touch();
    }

    public void completeMaintenance() {
        ensureStatus(BikeStatus.MAINTENANCE);
        status = BikeStatus.AVAILABLE;
        touch();
    }

    public void retire() {
        if (status == BikeStatus.RENTED ||
                status == BikeStatus.RESERVED) {
            throw new InvalidBikeStateException(
                    "Bike cannot be retired while " + status
            );
        }

        status = BikeStatus.RETIRED;
        touch();
    }

    private void ensureStatus(BikeStatus expected) {
        if (status != expected) {
            throw new InvalidBikeStateException(
                    "Expected bike status %s but was %s"
                            .formatted(expected, status)
            );
        }
    }

    private void validate() {
        if (serialNumber == null || serialNumber.isBlank()) {
            throw new IllegalArgumentException(
                    "Serial number cannot be blank"
            );
        }

        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException(
                    "Bike type cannot be blank"
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Bike status cannot be null"
            );
        }

        if (stationId == null) {
            throw new IllegalArgumentException(
                    "Station cannot be null"
            );
        }
    }

    private void touch() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getType() {
        return type;
    }

    public BikeStatus getStatus() {
        return status;
    }

    public void setStatus(BikeStatus status) {
        this.status = status;
    }

    public UUID getStationId() {
        return stationId;
    }

    public void setStationId(UUID stationId) {
        this.stationId = stationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
