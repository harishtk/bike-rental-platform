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
import java.time.Instant;
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
                .findByIdAndUserId(saved.getId(), saved.getUserId())
                .orElseThrow();

        assertThat(retrieved.getUserId()).isEqualTo(userId);
        assertThat(retrieved.getBikeId()).isEqualTo(bikeId);
        assertThat(retrieved.getStationId()).isEqualTo(stationId);
        assertThat(retrieved.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void shouldReturn404WhenReservationDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();

        assertThat(reservationRepository.findByIdAndUserId(reservationId, userId)).isEmpty();
    }

    @Test
    void shouldFindOnlyReservationsForUserIncludingHistory() {
        UUID userId = UUID.randomUUID();
        Reservation previous = Reservation.create(
                userId, UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(7));
        previous.cancel(Instant.now());
        Reservation cancelled = reservationRepository.save(previous);
        Reservation active = reservationRepository.save(Reservation.create(
                userId, UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(2)));
        Reservation other = reservationRepository.save(Reservation.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Duration.ofDays(2)));

        assertThat(reservationRepository.findByUserId(userId))
                .extracting(Reservation::getId)
                .containsExactlyInAnyOrder(cancelled.getId(), active.getId());
        assertThat(reservationRepository.findByUserId(other.getUserId()))
                .extracting(Reservation::getId).containsExactly(other.getId());
        assertThat(reservationRepository.findByUserId(UUID.randomUUID())).isEmpty();
        assertThat(reservationRepository.findByIdAndUserId(active.getId(), other.getUserId()))
                .isEmpty();
    }

    @Test
    void shouldCancelReservation() {
        Reservation reservation = reservationRepository.save(
                Reservation.create(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Duration.ofDays(3)
                )
        );

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);

        reservation.cancel(Instant.now());

        Reservation cancelled = reservationRepository.save(reservation);

        assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);
    }
}
