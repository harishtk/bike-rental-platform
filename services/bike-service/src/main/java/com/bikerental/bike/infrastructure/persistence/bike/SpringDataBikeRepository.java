package com.bikerental.bike.infrastructure.persistence.bike;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataBikeRepository extends
        JpaRepository<BikeEntity, UUID>, JpaSpecificationExecutor<BikeEntity> {

    boolean existsBySerialNumber(String serialNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT b
      FROM BikeEntity b
     WHERE b.id = :bikeId
    """)
    Optional<BikeEntity> findByIdForUpdate(
            @Param("bikeId") UUID bikeId
    );
}
