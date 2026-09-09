package com.bikerental.reservation.infrastructure.persistence.reservation;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;

@RequiredArgsConstructor
@Repository
public class ReservationRepositoryAdapter
        implements ReservationRepository {

    private final SpringDataReservationRepository repository;
    private final ReservationEntityMapper mapper;

    @Override
    public Reservation save(Reservation reservation) {

        ReservationEntity entity =
                mapper.toEntity(reservation);

        ReservationEntity savedEntity =
                repository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Reservation> findById(UUID reservationId) {

        return repository.findById(reservationId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByUserIdAndStatus(
            UUID userId,
            ReservationStatus status
    ) {
        return repository.existsByUserIdAndStatus(
                userId,
                status
        );
    }

    @Override
    public List<Reservation> findExpiredActiveReservations(
            Instant currentTime
    ) {
        return repository
                .findByStatusAndExpiresAtLessThanEqual(
                        ReservationStatus.ACTIVE,
                        currentTime
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Reservation> allReservations() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}