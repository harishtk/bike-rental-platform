package com.bikerental.rental.application.bike;

import java.util.UUID;

public interface BikeRentalGateway {

    BikeRentalDetails startRental(
            UUID bikeId,
            UUID operationId
    );

    void returnBike(
            UUID bikeId,
            UUID stationId,
            UUID operationId
    );
}