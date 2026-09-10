package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
public class ReservationServiceTest extends
        AbstractPostgresIntegrationTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BikeReservationGateway bikeReservationGateway;
    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldReleaseBikeWhenReservationSaveFails() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.ACTIVE))
                .thenReturn(false);

        when(bikeReservationGateway.reserveBike(bikeId))
                .thenReturn(new BikeReservationDetails(bikeId, stationId));

        when(reservationRepository.save(any(Reservation.class)))
                .thenThrow(new ActiveReservationAlreadyExistsException("Active reservation already exists"));

        assertThatThrownBy(() -> reservationService.createReservation(userId, bikeId, Duration.ofHours(24)))
                .isInstanceOf(ActiveReservationAlreadyExistsException.class);

        verify(bikeReservationGateway).reserveBike(bikeId);
        verify(bikeReservationGateway).releaseBike(bikeId);
    }
}
