package com.bikerental.bike.application.bike;

import com.bikerental.bike.application.idempotency.BikeOperation;
import com.bikerental.bike.application.idempotency.BikeOperationRepository;
import com.bikerental.bike.application.idempotency.BikeOperationType;
import com.bikerental.bike.application.idempotency.IdempotencyKeyConflictException;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BikeReservationCommandService {

    private final BikeRepository bikeRepository;
    private final BikeOperationRepository bikeOperationRepository;
    private final Clock clock;

    @Transactional
    public BikeReservationResult reserveBike(
            UUID bikeId,
            UUID operationId
    ) {
        Optional<BikeOperation> existing =
                bikeOperationRepository.findById(operationId);

        if (existing.isPresent()) {
            return replayReserve(
                    bikeId,
                    existing.get()
            );
        }

        Bike bike =
                bikeRepository.findByIdForUpdate(bikeId)
                        .orElseThrow(() ->
                                new BikeNotFoundException(bikeId)
                        );

        /*
         * Important second check.
         *
         * Another request using the same operationId and bikeId may
         * have completed while this request was waiting for the
         * pessimistic bike lock.
         */
        existing =
                bikeOperationRepository.findById(operationId);

        if (existing.isPresent()) {
            return replayReserve(
                    bikeId,
                    existing.get()
            );
        }

        bike.reserve();

        Bike savedBike =
                bikeRepository.save(bike);

        Instant now = Instant.now(clock);

        bikeOperationRepository.saveAndFlush(
                BikeOperation.reserve(
                        operationId,
                        bikeId,
                        savedBike.getStationId(),
                        now
                )
        );

        return new BikeReservationResult(
                savedBike.getId(),
                savedBike.getStationId()
        );
    }

    @Transactional
    public Bike releaseBike(
            UUID bikeId,
            UUID operationId
    ) {
        Optional<BikeOperation> existing =
                bikeOperationRepository.findById(operationId);

        if (existing.isPresent()) {
            replayRelease(
                    bikeId,
                    existing.get()
            );

            return null;
        }

        Bike bike =
                bikeRepository.findByIdForUpdate(bikeId)
                        .orElseThrow(() ->
                                new BikeNotFoundException(bikeId)
                        );

        existing =
                bikeOperationRepository.findById(operationId);

        if (existing.isPresent()) {
            replayRelease(
                    bikeId,
                    existing.get()
            );

            return bike;
        }

        bike.release();

        bikeRepository.save(bike);

        bikeOperationRepository.saveAndFlush(
                BikeOperation.release(
                        operationId,
                        bikeId,
                        Instant.now(clock)
                )
        );

        return bike;
    }

    private BikeReservationResult replayReserve(
            UUID bikeId,
            BikeOperation operation
    ) {
        validateExistingOperation(
                operation,
                bikeId,
                BikeOperationType.RESERVE
        );

        return new BikeReservationResult(
                operation.bikeId(),
                operation.resultStationId()
        );
    }

    private void replayRelease(
            UUID bikeId,
            BikeOperation operation
    ) {
        validateExistingOperation(
                operation,
                bikeId,
                BikeOperationType.RELEASE
        );
    }

    private void validateExistingOperation(
            BikeOperation operation,
            UUID expectedBikeId,
            BikeOperationType expectedType
    ) {
        if (!operation.bikeId().equals(expectedBikeId)
                || operation.operationType() != expectedType) {

            throw new IdempotencyKeyConflictException(
                    operation.operationId()
            );
        }
    }
}