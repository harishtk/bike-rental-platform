package com.bikerental.bike.infrastructure.persistence.station;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataStationRepository
        extends JpaRepository<StationEntity, UUID> {
}
