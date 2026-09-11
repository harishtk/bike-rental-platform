package com.bikerental.reservation.application.bike;

public class BikeServiceUnavailableException extends RuntimeException {

    public BikeServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
