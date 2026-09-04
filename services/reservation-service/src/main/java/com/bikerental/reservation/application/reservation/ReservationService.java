package com.bikerental.reservation.application.reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bikerental.reservation.infrastructure.persistence.reservation.ReservationEntityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;

    public ReservationService(
            ReservationRepository reservationRepository, ReservationEntityMapper mapper
    ) {
        this.reservationRepository = reservationRepository;
    }

    @Transactional
    public Reservation createReservation(
            UUID userId,
            UUID bikeId,
            UUID stationId,
            Duration duration
    ) {
        boolean hasActiveReservation =
                reservationRepository.existsByUserIdAndStatus(
                        userId,
                        ReservationStatus.ACTIVE
                );

        if (hasActiveReservation) {
            throw new ActiveReservationAlreadyExistsException(
                    userId
            );
        }

        Reservation reservation =
                Reservation.create(
                        userId,
                        bikeId,
                        stationId,
                        duration
                );

        return reservationRepository.save(reservation);
    }

    @Transactional(readOnly = true)
    public Reservation getReservation(UUID reservationId) {

        return reservationRepository.findById(reservationId)
                .orElseThrow(() ->
                        new ReservationNotFoundException(reservationId)
                );
    }

    @Transactional(readOnly = true)
    public List<Reservation> allReservations() {
        return reservationRepository.allReservations();
    }

    @Transactional
    public Reservation cancelReservation(UUID reservationId) {

        Reservation reservation = getReservation(reservationId);

        reservation.cancel();

        return reservationRepository.save(reservation);
    }
}