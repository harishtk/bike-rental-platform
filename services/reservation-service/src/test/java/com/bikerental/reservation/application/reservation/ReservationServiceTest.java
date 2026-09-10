package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.bikerental.reservation.application.reservation.event.ReservationCreatedEvent;
import com.bikerental.reservation.application.reservation.event.ReservationEventTypes;
import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.domain.outbox.OutboxEventRepository;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
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
    @Mock
    private OutboxEventRepository outboxEventRepository;
    @Mock
    private EventPayloadSerializer eventPayloadSerializer;
    @InjectMocks
    private ReservationService reservationService;

    @Test
    void shouldCreateReservation() {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        when(reservationRepository.existsByUserIdAndStatus(userId, ReservationStatus.ACTIVE))
                .thenReturn(false);

        when(bikeReservationGateway.reserveBike(bikeId))
                .thenReturn(new BikeReservationDetails(bikeId, stationId));

        when(reservationRepository.save(any(Reservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(eventPayloadSerializer.serialize(any()))
                .thenReturn("{\"type\":\"reservation-created\"}");

        Reservation reservation = reservationService.createReservation(
                userId,
                bikeId,
                Duration.ofHours(24));

        verify(reservationRepository).save(any(Reservation.class));
        verify(reservationRepository).flush();
        verify(eventPayloadSerializer).serialize(any(ReservationCreatedEvent.class));
        verify(outboxEventRepository).save(any(OutboxEvent.class));

        ArgumentCaptor<OutboxEvent> outboxCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(outboxCaptor.capture());
        OutboxEvent savedOutbox = outboxCaptor.getValue();

        assertThat(savedOutbox.getAggregateType()).isEqualTo(ReservationEventTypes.AGGREGATE_TYPE);
        assertThat(savedOutbox.getAggregateId()).isEqualTo(reservation.getId());
        assertThat(savedOutbox.getEventType()).isEqualTo(ReservationEventTypes.CREATED);
    }

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
