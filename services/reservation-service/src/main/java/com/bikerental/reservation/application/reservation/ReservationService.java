package com.bikerental.reservation.application.reservation;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.infrastructure.persistence.reservation.ReservationEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;

@RequiredArgsConstructor
@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BikeReservationGateway bikeReservationGateway;

    @Transactional
    public Reservation createReservation(
            UUID userId,
            UUID bikeId,
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

        BikeReservationDetails bikeDetails =
                bikeReservationGateway.reserveBike(bikeId);

        try {
            Reservation reservation =
                    Reservation.create(
                            userId,
                            bikeId,
                            bikeDetails.stationId(),
                            duration
                    );

            return reservationRepository.save(reservation);
        } catch (RuntimeException e) {
            bikeReservationGateway.releaseBike(bikeId);
            throw e;
        }
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

        bikeReservationGateway.releaseBike(reservation.getBikeId());

        reservation.cancel(Instant.now());

        return reservationRepository.save(reservation);
    }
}