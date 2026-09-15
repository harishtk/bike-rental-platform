package com.bikerental.rental.infrastructure.persistence.client.bike;

import com.bikerental.rental.application.bike.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeignBikeRentalGateway
        implements BikeRentalGateway {

    private final BikeFeignClient client;

    @Override
    public void startRental(
            UUID bikeId,
            UUID operationId
    ) {
        client.rentBike(
                bikeId,
                operationId
        );
    }

    @Override
    public void returnBike(
            UUID bikeId,
            UUID stationId,
            UUID operationId
    ) {
        client.returnBike(
                bikeId,
                new ReturnBikeRequest(stationId),
                operationId
        );
    }
}