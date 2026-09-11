package com.bikerental.reservation.application.bike;

import java.util.UUID;

public interface BikeReservationGateway {

    BikeReservationDetails reserveBike(UUID bikeId, UUID operationId);

    void releaseBike(UUID bikeId, UUID operationId);
}
