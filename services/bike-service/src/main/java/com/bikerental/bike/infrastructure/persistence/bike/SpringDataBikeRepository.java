package com.bikerental.bike.infrastructure.persistence.bike;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface SpringDataBikeRepository extends
        JpaRepository<BikeEntity, UUID>, JpaSpecificationExecutor<BikeEntity> {

    boolean existsBySerialNumber(String serialNumber);
}
