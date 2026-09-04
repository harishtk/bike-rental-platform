package com.bikerental.reservation.integration.reservation;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Sql(
        scripts = "/sql/cleanup-reservations.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class ReservationRepositoryIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private ReservationRepository reservationRepository;

    @Test
    void shouldSaveAndRetrieveReservation() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        Reservation reservation = Reservation.create(
                userId,
                bikeId,
                stationId,
                Duration.ofDays(7)
        );

        Reservation saved = reservationRepository.save(reservation);

        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getBikeId()).isEqualTo(bikeId);
        assertThat(saved.getStationId()).isEqualTo(stationId);
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        Reservation retrieved = reservationRepository
                .findById(saved.getId())
                .orElseThrow();

        assertThat(retrieved.getUserId()).isEqualTo(userId);
        assertThat(retrieved.getBikeId()).isEqualTo(bikeId);
        assertThat(retrieved.getStationId()).isEqualTo(stationId);
        assertThat(retrieved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void shouldReturn404WhenReservationDoesNotExist() {
        UUID reservationId = UUID.randomUUID();

        assertThat(reservationRepository.findById(reservationId)).isEmpty();
    }

    @Test
    void shouldFindAllReservations() {
        reservationRepository.save(
                Reservation.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Duration.ofDays(7)
                )
        );

        reservationRepository.save(
                Reservation.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Duration.ofDays(2)
                )
        );
        List<Reservation> reservations = reservationRepository.allReservations();

        assertThat(reservations)
                .hasSize(2);
    }

}
