package com.bikerental.reservation.domain.reservation;

public class ActiveReservationAlreadyExistsException extends RuntimeException {

    public ActiveReservationAlreadyExistsException(String message) {
        super(message);
    }
}