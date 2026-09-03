package com.bikerental.bike.infrastructure.persistence.bike;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataBikeRepository extends
        JpaRepository<BikeEntity, UUID> {

    boolean existsBySerialNumber(String serialNumber);
}
