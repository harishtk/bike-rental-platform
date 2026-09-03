package com.bikerental.bike.application.bike;

public class DuplicateBikeSerialNumberException
        extends RuntimeException {

    public DuplicateBikeSerialNumberException(String serialNumber) {
        super("Bike serial number already exists: " + serialNumber);
    }
}