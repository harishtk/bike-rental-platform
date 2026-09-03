package com.bikerental.bike.application.bike;

import java.util.UUID;

public class BikeNotFoundException extends RuntimeException {

    public BikeNotFoundException(UUID bikeId) {
        super("Bike not found: " + bikeId);
    }
}
