package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.infrastructure.config.FeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "bike-service",
        url = "${clients.bike-service.url}",
        configuration = {FeignConfig.class}
)
public interface BikeFeignClient {

    @PostMapping("/api/v1/bikes/{bikeId}/reserve")
    BikeFeignResponse reserveBike(
            @PathVariable("bikeId") UUID bikeId,
            @RequestHeader("X-Idempotency-Key") UUID idempotencyKey
    );

    @PostMapping("/api/v1/bikes/{bikeId}/release")
    void releaseBike(
            @PathVariable("bikeId") UUID bikeId,
            @RequestHeader("X-Idempotency-Key") UUID idempotencyKey
    );
}
