package com.bikerental.rental.infrastructure.persistence.rental;

import com.bikerental.rental.domain.rental.RentalStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rentals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RentalEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "bike_id", nullable = false)
    private UUID bikeId;

    @Column(name = "start_station_id", nullable = false)
    private UUID startStationId;

    @Column(name = "return_station_id")
    private UUID returnStationId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RentalStatus status;

    @Column(name = "daily_rate", nullable = false)
    private BigDecimal dailyRate;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    public RentalEntity(
            UUID id,
            UUID userId,
            UUID bikeId,
            UUID startStationId,
            UUID returnStationId,
            Instant startedAt,
            Instant returnedAt,
            RentalStatus status,
            BigDecimal dailyRate,
            BigDecimal totalAmount,
            Instant createdAt,
            Instant updatedAt,
            Long version
    ) {
        this.id = id;
        this.userId = userId;
        this.bikeId = bikeId;
        this.startStationId = startStationId;
        this.returnStationId = returnStationId;
        this.startedAt = startedAt;
        this.returnedAt = returnedAt;
        this.status = status;
        this.dailyRate = dailyRate;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }
}
