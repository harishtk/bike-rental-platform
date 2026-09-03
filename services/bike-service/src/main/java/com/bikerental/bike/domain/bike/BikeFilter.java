package com.bikerental.bike.domain.bike;

import org.springframework.data.domain.Pageable;

import java.util.UUID;

public record BikeFilter(
        UUID stationId,
        BikeStatus bikeStatus,
        Pageable pageable
) {
}
