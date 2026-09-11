package com.bikerental.rental.infrastructure.persistence.client.bike;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "bike-service",
        url = "${clients.bike-service.url}"
)
public interface BikeFeignClient {

    @PostMapping("/api/v1/bikes/{bikeId}/rent")
    BikeFeignResponse rentBike(
            @PathVariable UUID bikeId,
            @RequestHeader("X-Idempotency-Key")
            UUID operationId
    );

    @PostMapping("/api/v1/bikes/{bikeId}/return")
    void returnBike(
            @PathVariable UUID bikeId,
            @RequestParam UUID stationId,
            @RequestHeader("X-Idempotency-Key")
            UUID operationId
    );
}