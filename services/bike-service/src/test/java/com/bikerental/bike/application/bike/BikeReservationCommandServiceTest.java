package com.bikerental.bike.application.bike;

import com.bikerental.bike.application.idempotency.BikeOperation;
import com.bikerental.bike.application.idempotency.BikeOperationRepository;
import com.bikerental.bike.application.idempotency.IdempotencyKeyConflictException;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BikeReservationCommandServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private BikeOperationRepository bikeOperationRepository;

    @Mock
    private Bike bike;

    private BikeReservationCommandService service;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock =
                Clock.fixed(
                        Instant.parse(
                                "2026-09-11T10:00:00Z"
                        ),
                        ZoneOffset.UTC
                );

        service =
                new BikeReservationCommandService(
                        bikeRepository,
                        bikeOperationRepository,
                        clock
                );
    }

    @Test
    void shouldReserveBikeAndStoreOperation() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.empty());

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.of(bike));

        when(bike.getId())
                .thenReturn(bikeId);

        when(bike.getStationId())
                .thenReturn(stationId);

        when(bikeRepository.save(bike))
                .thenReturn(bike);

        BikeReservationResult result =
                service.reserveBike(
                        bikeId,
                        operationId
                );

        assertThat(result.id())
                .isEqualTo(bikeId);

        assertThat(result.stationId())
                .isEqualTo(stationId);

        verify(bike).reserve();

        verify(bikeOperationRepository)
                .saveAndFlush(
                        BikeOperation.reserve(
                                operationId,
                                bikeId,
                                stationId,
                                Instant.now(clock)
                        )
                );
    }

    @Test
    void shouldReplaySuccessfulReserveWithSameOperationId() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existing =
                BikeOperation.reserve(
                        operationId,
                        bikeId,
                        stationId,
                        Instant.now(clock)
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existing));

        BikeReservationResult result =
                service.reserveBike(
                        bikeId,
                        operationId
                );

        assertThat(result.id())
                .isEqualTo(bikeId);

        assertThat(result.stationId())
                .isEqualTo(stationId);

        verifyNoInteractions(bikeRepository);

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldRejectOperationIdUsedForAnotherBike() {
        UUID firstBikeId = UUID.randomUUID();
        UUID secondBikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existing =
                BikeOperation.reserve(
                        operationId,
                        firstBikeId,
                        UUID.randomUUID(),
                        Instant.now(clock)
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.reserveBike(
                        secondBikeId,
                        operationId
                )
        ).isInstanceOf(
                IdempotencyKeyConflictException.class
        );

        verifyNoInteractions(bikeRepository);
    }

    @Test
    void shouldRejectReserveOperationIdUsedForRelease() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existing =
                BikeOperation.reserve(
                        operationId,
                        bikeId,
                        UUID.randomUUID(),
                        Instant.now(clock)
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() ->
                service.releaseBike(
                        bikeId,
                        operationId
                )
        ).isInstanceOf(
                IdempotencyKeyConflictException.class
        );

        verifyNoInteractions(bikeRepository);
    }

    @Test
    void shouldReleaseBikeAndStoreOperation() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.empty());

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.of(bike));

        when(bikeRepository.save(bike))
                .thenReturn(bike);

        service.releaseBike(
                bikeId,
                operationId
        );

        verify(bike).release();

        verify(bikeOperationRepository)
                .saveAndFlush(
                        BikeOperation.release(
                                operationId,
                                bikeId,
                                Instant.now(clock)
                        )
                );
    }

    @Test
    void shouldReplayReleaseWithoutMutatingBikeAgain() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existing =
                BikeOperation.release(
                        operationId,
                        bikeId,
                        Instant.now(clock)
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existing));

        service.releaseBike(
                bikeId,
                operationId
        );

        verifyNoInteractions(bikeRepository);

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());
    }
}