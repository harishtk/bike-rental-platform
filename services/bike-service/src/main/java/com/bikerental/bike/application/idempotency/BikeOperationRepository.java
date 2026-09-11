package com.bikerental.bike.application.idempotency;

import java.util.Optional;
import java.util.UUID;

public interface BikeOperationRepository {

    Optional<BikeOperation> findById(UUID operationId);

    BikeOperation saveAndFlush(BikeOperation operation);
}