package com.bikerental.bike.domain.bike;

public class InvalidBikeStateException extends RuntimeException {

    public InvalidBikeStateException(String message) {
        super(message);
    }
}