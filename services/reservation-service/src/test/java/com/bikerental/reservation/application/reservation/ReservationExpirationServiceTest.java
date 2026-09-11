package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.bikerental.reservation.application.reservation.event.ReservationOutboxEventFactory;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-01-04T10:00:00Z");

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BikeReservationGateway bikeReservationGateway;
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private EventPayloadSerializer eventPayloadSerializer;
    private ReservationOutboxEventFactory reservationOutboxEventFactory;
    @MockitoBean
    private Clock clock;

    private ReservationExpirationService expirationService;

    private UUID reservationId;
    private UUID userId;
    private UUID bikeId;
    private UUID stationId;

    private Instant reservedAt;
    private Instant expiresAt;
    private Instant currentTime;

    @BeforeEach
    public void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        reservationOutboxEventFactory = new ReservationOutboxEventFactory(eventPayloadSerializer);
        reservationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        bikeId = UUID.randomUUID();
        stationId = UUID.randomUUID();

        expirationService = new ReservationExpirationService(
                reservationRepository,
                bikeReservationGateway,
                clock,
                outboxEventRepository,
                reservationOutboxEventFactory
        );

        reservedAt = Instant.parse("2026-01-01T10:00:00Z");
        expiresAt = Instant.parse("2026-01-03T10:00:00Z");
        currentTime = Instant.parse("2026-01-04T10:00:00Z");
    }

    @Test
    void shouldExpireExpiredReservationAndReleaseBike() {

        Reservation reservation = createReservation();

        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of(reservation));

        expirationService.expireReservations();

        verify(bikeReservationGateway).releaseBike(eq(bikeId), any(UUID.class));
        verify(reservationRepository).save(reservation);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(reservation.getUpdatedAt()).isNotNull();
    }

    @Test
    void shouldDoNothingWhenThereAreNoExpiredReservations() {
        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of());

        expirationService.expireReservations();

        verifyNoInteractions(bikeReservationGateway);
        verify(reservationRepository).findExpiredActiveReservations(any(Instant.class));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void shouldExpireMultipleReservationsAndReleaseEachBike() {
        UUID secondBikeId = UUID.randomUUID();

        Reservation firstReservation = createReservation();
        Reservation secondReservation = Reservation.restore(
                UUID.randomUUID(),
                userId,
                secondBikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of(firstReservation, secondReservation));

        expirationService.expireReservations();

        verify(bikeReservationGateway).releaseBike(eq(bikeId), any(UUID.class));
        verify(bikeReservationGateway).releaseBike(eq(secondBikeId), any(UUID.class));

        verify(reservationRepository).save(firstReservation);
        verify(reservationRepository).save(secondReservation);

        assertThat(firstReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(secondReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void shouldNotSaveReservationWhenBikeReleaseFails() {
        Reservation reservation = createReservation();
        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of(reservation));
        doThrow(new RuntimeException("Bike service unavailable"))
                .when(bikeReservationGateway)
                .releaseBike(eq(bikeId), any(UUID.class));

        assertDoesNotThrow(() -> expirationService.expireReservations());

        verify(bikeReservationGateway).releaseBike(eq(bikeId), any(UUID.class));
        verify(reservationRepository, never()).save(reservation);

        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void shouldContinueProcessingOtherReservationWhenOneReleaseFails() {
        UUID secondBikeId = UUID.randomUUID();

        Reservation firstReservation = createReservation();
        Reservation secondReservation = Reservation.restore(
                UUID.randomUUID(),
                userId,
                secondBikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of(firstReservation, secondReservation));
        doThrow(new RuntimeException("Bike service unavailable"))
                .when(bikeReservationGateway)
                .releaseBike(eq(bikeId), any(UUID.class));

        assertDoesNotThrow(() -> expirationService.expireReservations());

        verify(bikeReservationGateway).releaseBike(eq(bikeId), any(UUID.class));
        verify(bikeReservationGateway).releaseBike(eq(secondBikeId), any(UUID.class));

        verify(reservationRepository).save(secondReservation);
        verify(reservationRepository, never()).save(firstReservation);

        assertThat(firstReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(secondReservation.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
    }

    @Test
    void shouldOnlyQueryExpiredActiveReservations() {
        when(reservationRepository.findExpiredActiveReservations(any(Instant.class)))
                .thenReturn(List.of());

        expirationService.expireReservations();

        verify(reservationRepository).findExpiredActiveReservations(any(Instant.class));
        verifyNoInteractions(bikeReservationGateway);
    }

    private Reservation createReservation() {

        return Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );
    }
}