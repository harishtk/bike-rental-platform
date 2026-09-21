package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.application.reservation.event.ReservationOutboxEventFactory;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.Optional;
import com.bikerental.reservation.domain.reservation.InvalidReservationStateException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private BikeReservationGateway bikeReservationGateway;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ReservationOutboxEventFactory reservationOutboxEventFactory;

    private Clock clock;

    private ReservationService reservationService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-09-11T10:00:00Z"),
                ZoneOffset.UTC
        );

        reservationService = new ReservationService(
                reservationRepository,
                bikeReservationGateway,
                outboxEventRepository,
                reservationOutboxEventFactory,
                clock
        );
    }

    @Test
    void shouldCreateReservationAndStoreOutboxEvent() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        OutboxEvent outboxEvent = mock(OutboxEvent.class);

        when(reservationRepository.existsByUserIdAndStatus(
                userId,
                ReservationStatus.ACTIVE
        )).thenReturn(false);

        when(bikeReservationGateway.reserveBike(eq(bikeId), any(UUID.class)))
                .thenReturn(
                        new BikeReservationDetails(
                                bikeId,
                                stationId
                        )
                );

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(reservationOutboxEventFactory.created(any(Reservation.class)))
                .thenReturn(outboxEvent);

        Reservation reservation =
                reservationService.createReservation(
                        userId,
                        bikeId,
                        Duration.ofHours(24)
                );

        assertThat(reservation.getUserId())
                .isEqualTo(userId);

        assertThat(reservation.getBikeId())
                .isEqualTo(bikeId);

        assertThat(reservation.getStationId())
                .isEqualTo(stationId);

        assertThat(reservation.getStatus())
                .isEqualTo(ReservationStatus.ACTIVE);

        verify(bikeReservationGateway)
                .reserveBike(eq(bikeId), any(UUID.class));

        verify(reservationRepository)
                .save(any(Reservation.class));

        verify(reservationRepository)
                .flush();

        verify(reservationOutboxEventFactory)
                .created(reservation);

        verify(outboxEventRepository)
                .save(outboxEvent);

        verify(bikeReservationGateway, never())
                .releaseBike(any(UUID.class), any(UUID.class));
    }

    @Test
    void shouldRejectReservationWhenUserAlreadyHasActiveReservation() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();

        when(reservationRepository.existsByUserIdAndStatus(
                userId,
                ReservationStatus.ACTIVE
        )).thenReturn(true);

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        userId,
                        bikeId,
                        Duration.ofHours(24)
                )
        ).isInstanceOf(
                ActiveReservationAlreadyExistsException.class
        );

        verify(bikeReservationGateway, never())
                .reserveBike(any(UUID.class), any(UUID.class));

        verify(reservationRepository, never())
                .save(any());

        verify(outboxEventRepository, never())
                .save(any());
    }

    @Test
    void shouldReleaseBikeWhenReservationSaveFails() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        when(reservationRepository.existsByUserIdAndStatus(
                userId,
                ReservationStatus.ACTIVE
        )).thenReturn(false);

        when(bikeReservationGateway.reserveBike(eq(bikeId), any(UUID.class)))
                .thenReturn(
                        new BikeReservationDetails(
                                bikeId,
                                stationId
                        )
                );

        when(reservationRepository.save(any(Reservation.class)))
                .thenThrow(
                        new ActiveReservationAlreadyExistsException(
                                "Active reservation already exists"
                        )
                );

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        userId,
                        bikeId,
                        Duration.ofHours(24)
                )
        ).isInstanceOf(
                ActiveReservationAlreadyExistsException.class
        );

        verify(bikeReservationGateway)
                .reserveBike(eq(bikeId), any(UUID.class));

        verify(bikeReservationGateway)
                .releaseBike(eq(bikeId), any(UUID.class));

        verify(reservationOutboxEventFactory, never())
                .created(any());

        verify(outboxEventRepository, never())
                .save(any());
    }

    @Test
    void shouldReleaseBikeWhenOutboxSaveFails() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        OutboxEvent outboxEvent = mock(OutboxEvent.class);

        when(reservationRepository.existsByUserIdAndStatus(
                userId,
                ReservationStatus.ACTIVE
        )).thenReturn(false);

        when(bikeReservationGateway.reserveBike(eq(bikeId), any(UUID.class)))
                .thenReturn(
                        new BikeReservationDetails(
                                bikeId,
                                stationId
                        )
                );

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(reservationOutboxEventFactory.created(any(Reservation.class)))
                .thenReturn(outboxEvent);

        when(outboxEventRepository.save(outboxEvent))
                .thenThrow(
                        new RuntimeException(
                                "Failed to persist outbox event"
                        )
                );

        assertThatThrownBy(() ->
                reservationService.createReservation(
                        userId,
                        bikeId,
                        Duration.ofHours(24)
                )
        ).isInstanceOf(RuntimeException.class)
                .hasMessage(
                        "Failed to persist outbox event"
                );

        verify(reservationRepository)
                .flush();

        verify(outboxEventRepository)
                .save(outboxEvent);

        verify(bikeReservationGateway)
                .releaseBike(eq(bikeId), any(UUID.class));
    }

    @Test
    void shouldRejectUnownedCancellationBeforeAnySideEffects() {
        UUID reservationId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        when(reservationRepository.findByIdAndUserId(reservationId, customerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancelReservation(reservationId, customerId))
                .isInstanceOf(ReservationNotFoundException.class);

        verify(reservationRepository).findByIdAndUserId(reservationId, customerId);
        verifyNoMoreInteractions(reservationRepository);
        verifyNoInteractions(bikeReservationGateway, outboxEventRepository, reservationOutboxEventFactory);
    }

    @Test
    void shouldRejectInactiveCancellationBeforeReleasingBike() {
        Reservation reservation = Reservation.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Duration.ofHours(24));
        reservation.cancel(Instant.now(clock));
        when(reservationRepository.findByIdAndUserId(reservation.getId(), reservation.getUserId()))
                .thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancelReservation(
                reservation.getId(), reservation.getUserId()))
                .isInstanceOf(InvalidReservationStateException.class);

        verify(reservationRepository).findByIdAndUserId(reservation.getId(), reservation.getUserId());
        verifyNoMoreInteractions(reservationRepository);
        verifyNoInteractions(bikeReservationGateway, outboxEventRepository, reservationOutboxEventFactory);
    }
}