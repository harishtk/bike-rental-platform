package com.bikerental.bike.application.bike;

import com.bikerental.bike.application.idempotency.BikeOperation;
import com.bikerental.bike.application.idempotency.BikeOperationRepository;
import com.bikerental.bike.application.idempotency.BikeOperationType;
import com.bikerental.bike.application.idempotency.IdempotencyKeyConflictException;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeRepository;
import com.bikerental.bike.domain.bike.BikeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BikeRentalCommandServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private BikeOperationRepository bikeOperationRepository;

    private Clock clock;

    private BikeRentalCommandService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(
                Instant.parse("2026-09-12T10:00:00Z"),
                ZoneOffset.UTC
        );

        service = new BikeRentalCommandService(
                bikeRepository,
                bikeOperationRepository,
                clock
        );
    }

    @Test
    void shouldRentAvailableBike() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Bike bike = createAvailableBike(
                bikeId,
                stationId
        );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.empty());

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.of(bike));

        service.rentBike(
                bikeId,
                operationId
        );

        assertEquals(
                BikeStatus.RENTED,
                bike.getStatus()
        );

        verify(bikeRepository)
                .save(bike);

        ArgumentCaptor<BikeOperation> captor =
                ArgumentCaptor.forClass(BikeOperation.class);

        verify(bikeOperationRepository)
                .saveAndFlush(captor.capture());

        BikeOperation operation =
                captor.getValue();

        assertEquals(
                operationId,
                operation.operationId()
        );

        assertEquals(
                bikeId,
                operation.bikeId()
        );

        assertEquals(
                BikeOperationType.RENT,
                operation.operationType()
        );

        assertNull(
                operation.resultStationId()
        );

        assertEquals(
                Instant.parse("2026-09-12T10:00:00Z"),
                operation.processedAt()
        );
    }

    @Test
    void shouldReturnRentedBikeToStation() {
        UUID bikeId = UUID.randomUUID();
        UUID originalStationId = UUID.randomUUID();
        UUID returnStationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Bike bike = createRentedBike(
                bikeId,
                originalStationId
        );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.empty());

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.of(bike));

        service.returnBike(
                bikeId,
                returnStationId,
                operationId
        );

        assertEquals(
                BikeStatus.AVAILABLE,
                bike.getStatus()
        );

        assertEquals(
                returnStationId,
                bike.getStationId()
        );

        verify(bikeRepository)
                .save(bike);

        ArgumentCaptor<BikeOperation> captor =
                ArgumentCaptor.forClass(BikeOperation.class);

        verify(bikeOperationRepository)
                .saveAndFlush(captor.capture());

        BikeOperation operation =
                captor.getValue();

        assertEquals(
                operationId,
                operation.operationId()
        );

        assertEquals(
                bikeId,
                operation.bikeId()
        );

        assertEquals(
                BikeOperationType.RETURN,
                operation.operationType()
        );

        assertEquals(
                returnStationId,
                operation.resultStationId()
        );
    }

    @Test
    void shouldReplayExistingRentOperationWithoutRentingAgain() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation operation =
                BikeOperation.rent(
                        operationId,
                        bikeId,
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(operation));

        service.rentBike(
                bikeId,
                operationId
        );

        verifyNoInteractions(bikeRepository);

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldReplayExistingReturnOperationWithoutReturningAgain() {
        UUID bikeId = UUID.randomUUID();
        UUID returnStationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation operation =
                BikeOperation.returnBike(
                        operationId,
                        bikeId,
                        returnStationId,
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(operation));

        service.returnBike(
                bikeId,
                returnStationId,
                operationId
        );

        verifyNoInteractions(bikeRepository);

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldRejectRentWhenOperationIdWasUsedForDifferentBike() {
        UUID bikeId = UUID.randomUUID();
        UUID anotherBikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existingOperation =
                BikeOperation.rent(
                        operationId,
                        anotherBikeId,
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existingOperation));

        assertThrows(
                IdempotencyKeyConflictException.class,
                () -> service.rentBike(
                        bikeId,
                        operationId
                )
        );

        verifyNoInteractions(bikeRepository);
    }

    @Test
    void shouldRejectRentWhenOperationIdWasUsedForDifferentOperationType() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existingOperation =
                BikeOperation.reserve(
                        operationId,
                        bikeId,
                        UUID.randomUUID(),
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existingOperation));

        assertThrows(
                IdempotencyKeyConflictException.class,
                () -> service.rentBike(
                        bikeId,
                        operationId
                )
        );

        verifyNoInteractions(bikeRepository);
    }

    @Test
    void shouldRejectReturnWhenOperationIdWasUsedForRent() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        BikeOperation existingOperation =
                BikeOperation.rent(
                        operationId,
                        bikeId,
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.of(existingOperation));

        assertThrows(
                IdempotencyKeyConflictException.class,
                () -> service.returnBike(
                        bikeId,
                        stationId,
                        operationId
                )
        );

        verifyNoInteractions(bikeRepository);
    }

    @Test
    void shouldThrowWhenBikeDoesNotExistDuringRent() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(Optional.empty());

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.empty());

        assertThrows(
                BikeNotFoundException.class,
                () -> service.rentBike(
                        bikeId,
                        operationId
                )
        );

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());
    }

    @Test
    void shouldCheckOperationAgainAfterBikeLock() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Bike bike = createAvailableBike(
                bikeId,
                stationId
        );

        BikeOperation operation =
                BikeOperation.rent(
                        operationId,
                        bikeId,
                        clock.instant()
                );

        when(bikeOperationRepository.findById(operationId))
                .thenReturn(
                        Optional.empty(),
                        Optional.of(operation)
                );

        when(bikeRepository.findByIdForUpdate(bikeId))
                .thenReturn(Optional.of(bike));

        service.rentBike(
                bikeId,
                operationId
        );

        verify(bikeOperationRepository, times(2))
                .findById(operationId);

        verify(bikeRepository, never())
                .save(any());

        verify(bikeOperationRepository, never())
                .saveAndFlush(any());

        assertEquals(
                BikeStatus.AVAILABLE,
                bike.getStatus()
        );
    }

    private Bike createAvailableBike(
            UUID bikeId,
            UUID stationId
    ) {
        /*
         * Replace this with your actual Bike constructor/factory.
         */
        return Bike.create(
                "Bike-" + UUID.randomUUID(),
                "SN-" + bikeId,
                stationId
        );
    }

    private Bike createRentedBike(
            UUID bikeId,
            UUID stationId
    ) {
        Bike bike = createAvailableBike(
                bikeId,
                stationId
        );

        bike.rent();

        return bike;
    }
}