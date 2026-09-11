package com.bikerental.bike.infrastructure.persistence.idempotency;

import com.bikerental.bike.application.idempotency.BikeOperationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bike_operations")
@Getter
@NoArgsConstructor
public class BikeOperationEntity {

    @Id
    @Column(name = "operation_id", nullable = false)
    private UUID operationId;

    @Column(name = "bike_id", nullable = false)
    private UUID bikeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false)
    private BikeOperationType operationType;

    @Column(name = "result_station_id")
    private UUID resultStationId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    public BikeOperationEntity(
            UUID operationId,
            UUID bikeId,
            BikeOperationType operationType,
            UUID resultStationId,
            Instant processedAt
    ) {
        this.operationId = operationId;
        this.bikeId = bikeId;
        this.operationType = operationType;
        this.resultStationId = resultStationId;
        this.processedAt = processedAt;
    }
}