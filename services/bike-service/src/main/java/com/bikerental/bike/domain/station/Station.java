package com.bikerental.bike.domain.station;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Data
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

}
