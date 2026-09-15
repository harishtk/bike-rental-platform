package com.bikerental.rental.application.rental;

import com.bikerental.rental.application.bike.BikeRentalDetails;
import com.bikerental.rental.application.bike.BikeRentalGateway;
import com.bikerental.rental.application.pricing.RentalPricingService;
import com.bikerental.rental.domain.rental.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RentalService {

    private final RentalRepository rentalRepository;
    private final BikeRentalGateway bikeRentalGateway;
    private final RentalPricingService pricingService;
    private final Clock clock;

    @Transactional
    public Rental startRental(
            UUID userId,
            UUID bikeId,
            UUID stationId,
            BigDecimal dailyRate
    ) {
        if (rentalRepository.existsByUserIdAndStatus(
                userId,
                RentalStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "User already has an active rental"
            );
        }

        if (rentalRepository.existsByBikeIdAndStatus(
                bikeId,
                RentalStatus.ACTIVE
        )) {
            throw new IllegalStateException(
                    "Bike already has an active rental"
            );
        }

        UUID operationId = UUID.randomUUID();

        bikeRentalGateway.startRental(
                bikeId,
                operationId
        );

        Rental rental =
                Rental.start(
                        userId,
                        bikeId,
                        stationId,
                        dailyRate,
                        Instant.now(clock)
                );

        return rentalRepository.save(rental);
    }

    @Transactional
    public Rental returnRental(
            UUID rentalId,
            UUID stationId
    ) {
        Rental rental =
                rentalRepository.findById(rentalId)
                        .orElseThrow();

        Instant now =
                Instant.now(clock);

        BigDecimal total =
                pricingService.calculate(
                        rental.getStartedAt(),
                        now,
                        rental.getDailyRate()
                );

        UUID operationId = UUID.randomUUID();

        bikeRentalGateway.returnBike(
                rental.getBikeId(),
                stationId,
                operationId
        );

        rental.returnBike(
                stationId,
                now,
                total
        );

        return rentalRepository.save(rental);
    }

    @Transactional
    public Rental completeRental(UUID rentalId) {
        Rental rental =
                rentalRepository.findById(rentalId)
                        .orElseThrow(() -> new RentalNotFoundException(rentalId));

        rental.complete(
                Instant.now(clock)
        );

        return rentalRepository.save(rental);
    }
}