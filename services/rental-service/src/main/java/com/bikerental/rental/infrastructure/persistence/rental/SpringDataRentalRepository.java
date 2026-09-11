package com.bikerental.rental.infrastructure.persistence.rental;

import com.bikerental.rental.domain.rental.RentalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataRentalRepository
        extends JpaRepository<RentalEntity, UUID> {

    boolean existsByUserIdAndStatus(
            UUID userId,
            RentalStatus status
    );

    boolean existsByBikeIdAndStatus(
            UUID bikeId,
            RentalStatus status
    );
}