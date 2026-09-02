package com.bikerental.bike.domain.station;

import java.time.Instant;
import java.util.UUID;

public class Station {
    private UUID id;
    private String name;
    private String address;
    private int capacity;
    private StationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Station(
            UUID id,
            String name,
            String address,
            int capacity,
            StationStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.capacity = capacity;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        validate();
    }

    public static Station create(
            String name,
            String address,
            int capacity
    ) {
        Instant now = Instant.now();

        return new Station(
                UUID.randomUUID(),
                name,
                address,
                capacity,
                StationStatus.ACTIVE,
                now,
                now
        );
    }

    private void validate() {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Station name cannot be blank");
        }

        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Station address cannot be blank");
        }

        if (capacity <= 0) {
            throw new IllegalArgumentException("Station capacity must be greater than zero");
        }

        if (status == null) {
            throw new IllegalArgumentException("Station status cannot be null");
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
