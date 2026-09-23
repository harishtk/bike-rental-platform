package com.bikerental.rental.infrastructure.persistence.client.bike;

import com.bikerental.rental.infrastructure.config.BikeClientSecurityConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "bike-service",
        configuration = {
            BikeClientSecurityConfiguration.class
        }
)
public interface BikeFeignClient {

    @PostMapping("/api/v1/bikes/{bikeId}/rent")
    void rentBike(
            @PathVariable UUID bikeId,
            @RequestHeader("X-Idempotency-Key") UUID operationId
    );

    @PostMapping("/api/v1/bikes/{bikeId}/return")
    void returnBike(
            @PathVariable UUID bikeId,
            @RequestBody ReturnBikeRequest request,
            @RequestHeader("X-Idempotency-Key") UUID operationId
    );
}