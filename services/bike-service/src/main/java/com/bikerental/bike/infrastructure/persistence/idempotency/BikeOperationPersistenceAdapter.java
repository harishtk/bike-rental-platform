package com.bikerental.bike.infrastructure.persistence.idempotency;

import com.bikerental.bike.application.idempotency.BikeOperation;
import com.bikerental.bike.application.idempotency.BikeOperationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BikeOperationPersistenceAdapter
        implements BikeOperationRepository {

    private final SpringDataBikeOperationRepository repository;

    @Override
    public Optional<BikeOperation> findById(UUID operationId) {
        return repository.findById(operationId)
                .map(this::toDomain);
    }

    @Override
    public BikeOperation saveAndFlush(BikeOperation operation) {
        BikeOperationEntity entity =
                new BikeOperationEntity(
                        operation.operationId(),
                        operation.bikeId(),
                        operation.operationType(),
                        operation.resultStationId(),
                        operation.processedAt()
                );

        return toDomain(
                repository.saveAndFlush(entity)
        );
    }

    private BikeOperation toDomain(
            BikeOperationEntity entity
    ) {
        return new BikeOperation(
                entity.getOperationId(),
                entity.getBikeId(),
                entity.getOperationType(),
                entity.getResultStationId(),
                entity.getProcessedAt()
        );
    }
}