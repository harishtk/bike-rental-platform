package com.bikerental.reservation.infrastructure.scheduler;

import com.bikerental.reservation.application.reservation.ReservationExpirationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ReservationExpirationScheduler {

    private final ReservationExpirationService reservationExpirationService;


    @Scheduled(
            fixedDelayString = "${reservation.expiration.fixed-delay:60000}"
    )
    public void expireReservations() {
        reservationExpirationService.expireReservations();
    }
}
