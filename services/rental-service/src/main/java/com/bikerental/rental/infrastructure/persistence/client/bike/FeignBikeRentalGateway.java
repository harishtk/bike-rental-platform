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
    public BikeRentalDetails startRental(
            UUID bikeId,
            UUID operationId
    ) {
        BikeFeignResponse response =
                client.rentBike(
                        bikeId,
                        operationId
                );

        return new BikeRentalDetails(
                response.id(),
                response.stationId()
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
                stationId,
                operationId
        );
    }
}