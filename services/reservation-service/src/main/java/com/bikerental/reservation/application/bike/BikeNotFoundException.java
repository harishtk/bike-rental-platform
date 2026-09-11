package com.bikerental.reservation.application.bike;

import java.util.UUID;

public class BikeNotFoundException extends RuntimeException {
    public BikeNotFoundException(UUID bikeId) {
        super("Could not find bike with id: " + bikeId);
    }
}

