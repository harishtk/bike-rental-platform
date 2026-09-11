package com.bikerental.bike.application.idempotency;

import java.util.UUID;

public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException(UUID operationId) {
        super(
                "Idempotency key has already been used for another operation: "
                        + operationId
        );
    }
}