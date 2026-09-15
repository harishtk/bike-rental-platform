package com.bikerental.rental.infrastructure.persistence.rental;

import com.bikerental.rental.domain.rental.Rental;
import com.bikerental.rental.domain.rental.RentalRepository;
import com.bikerental.rental.domain.rental.RentalStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RentalPersistenceAdapter
        implements RentalRepository {

    private final SpringDataRentalRepository repository;
    private final RentalMapper mapper;

    @Override
    @Transactional
    public Rental save(Rental rental) {
        return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(rental)));
    }

    @Override
    public Optional<Rental> findById(UUID rentalId) {
        return repository.findById(rentalId)
                .map(mapper::toDomain);
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
}