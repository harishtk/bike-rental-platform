package com.bikerental.rental.domain.rental;

import java.util.Optional;
import java.util.UUID;

public interface RentalRepository {

    Rental save(Rental rental);

    Optional<Rental> findById(UUID rentalId);

    boolean existsByUserIdAndStatus(
            UUID userId,
            RentalStatus status
    );

    boolean existsByBikeIdAndStatus(
            UUID bikeId,
            RentalStatus status
    );
}