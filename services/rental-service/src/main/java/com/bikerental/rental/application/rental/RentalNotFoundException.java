package com.bikerental.rental.application.rental;

import java.util.UUID;

public class RentalNotFoundException extends RuntimeException {
    public RentalNotFoundException(UUID rentalId) {
        super("Rental not found for id: " + rentalId);
    }
}
