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
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class BikeRentalCommandService {

    private final BikeRepository bikeRepository;
    private final BikeOperationRepository bikeOperationRepository;
    private final Clock  clock;

    @Transactional
    public void rentBike(
            UUID bikeId,
            UUID operationId
    ) {
        var existingOperation =  bikeOperationRepository.findById(operationId);

        if (existingOperation.isPresent()) {
            validateExistingBikeOperation(
                    existingOperation.get(),
                    bikeId,
                    BikeOperationType.RENT
            );
            return;
        }

        Bike bike =
                bikeRepository.findByIdForUpdate(bikeId)
                        .orElseThrow(() -> new BikeNotFoundException(bikeId));

        existingOperation = bikeOperationRepository.findById(operationId);

        if (existingOperation.isPresent()) {
            validateExistingBikeOperation(
                    existingOperation.get(),
                    bikeId,
                    BikeOperationType.RENT
            );
            return;
        }

        bike.rent();

        bikeRepository.save(bike);

        bikeOperationRepository.saveAndFlush(
                BikeOperation.rent(
                        operationId,
                        bikeId,
                        clock.instant()
                )
        );
    }

    @Transactional
    public void returnBike(
            UUID bikeId,
            UUID stationId,
            UUID operationId
    ) {
        var existingOperation =  bikeOperationRepository.findById(operationId);

        if (existingOperation.isPresent()) {
            validateExistingBikeOperation(
                    existingOperation.get(),
                    bikeId,
                    BikeOperationType.RETURN
            );
            return;
        }

        Bike bike =
                bikeRepository.findByIdForUpdate(bikeId)
                        .orElseThrow(() -> new BikeNotFoundException(bikeId));

        existingOperation = bikeOperationRepository.findById(operationId);

        if (existingOperation.isPresent()) {
            validateExistingBikeOperation(
                    existingOperation.get(),
                    bikeId,
                    BikeOperationType.RETURN
            );
            return;
        }

        bike.returnToStation(stationId);

        bikeRepository.save(bike);

        bikeOperationRepository.saveAndFlush(
                BikeOperation.returnBike(
                        operationId,
                        bikeId,
                        stationId,
                        clock.instant()
                )
        );
    }

    private void validateExistingBikeOperation(
            BikeOperation operation,
            UUID expectedBikeId,
            BikeOperationType expectedType
    ) {
        if (!operation.bikeId().equals(expectedBikeId) ||
                !operation.operationType().equals(expectedType)) {
            throw new IdempotencyKeyConflictException(
                    operation.operationId()
            );
        }
    }
}
