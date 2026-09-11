package com.bikerental.rental.infrastructure.persistence.rental;

import com.bikerental.rental.domain.rental.Rental;
import com.bikerental.rental.domain.rental.RentalRepository;
import com.bikerental.rental.domain.rental.RentalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RentalPersistenceAdapter
        implements RentalRepository {

    private final SpringDataRentalRepository repository;

    @Override
    public Rental save(Rental rental) {
        return toDomain(
                repository.save(
                        toEntity(rental)
                )
        );
    }

    @Override
    public Optional<Rental> findById(UUID rentalId) {
        return repository.findById(rentalId)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUserIdAndStatus(
            UUID userId,
            RentalStatus status
    ) {
        return repository.existsByUserIdAndStatus(
                userId,
                status
        );
    }

    @Override
    public boolean existsByBikeIdAndStatus(
            UUID bikeId,
            RentalStatus status
    ) {
        return repository.existsByBikeIdAndStatus(
                bikeId,
                status
        );
    }

    private RentalEntity toEntity(Rental rental) {
        return new RentalEntity(
                rental.getId(),
                rental.getUserId(),
                rental.getBikeId(),
                rental.getStartStationId(),
                rental.getReturnStationId(),
                rental.getStartedAt(),
                rental.getReturnedAt(),
                rental.getStatus(),
                rental.getDailyRate(),
                rental.getTotalAmount(),
                rental.getCreatedAt(),
                rental.getUpdatedAt()
        );
    }

    private Rental toDomain(RentalEntity entity) {
        return Rental.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getBikeId(),
                entity.getStartStationId(),
                entity.getReturnStationId(),
                entity.getStartedAt(),
                entity.getReturnedAt(),
                entity.getStatus(),
                entity.getDailyRate(),
                entity.getTotalAmount(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}