package com.bikerental.bike.infrastructure.persistence.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataBikeOperationRepository
        extends JpaRepository<BikeOperationEntity, UUID> {
}