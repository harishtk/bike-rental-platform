package com.bikerental.bike.infrastructure.persistence.bike;

import com.bikerental.bike.domain.bike.BikeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "bikes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_bikes_serial_number",
                        columnNames = "serial_number"
                )
        }
)
public class BikeEntity {

    @Id
    private UUID id;

    @Column(name = "serial_number", nullable = false, length = 100)
    private String serialNumber;

    @Column(nullable = false, length = 100)
    private String type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BikeStatus status;

    @Column(name = "station_id", nullable = false)
    private UUID stationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

}
