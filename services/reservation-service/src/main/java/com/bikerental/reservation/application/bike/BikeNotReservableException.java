package com.bikerental.reservation.application.bike;

import java.util.UUID;

public class BikeNotReservableException extends RuntimeException {
    public BikeNotReservableException(UUID bikeId) {
        super("Could not reserve bike with id: " + bikeId);
    }
}
