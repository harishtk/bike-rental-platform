package com.bikerental.reservation.application.reservation;

import java.util.UUID;

public class ActiveReservationAlreadyExistsException
        extends RuntimeException {

    public ActiveReservationAlreadyExistsException(UUID userId) {
        super("User already has an active reservation: " + userId);
    }

    public ActiveReservationAlreadyExistsException(String message) {
        super(message);
    }
}