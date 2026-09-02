package com.bikerental.bike.application.station;

import java.util.UUID;

public class StationNotFoundException extends RuntimeException {

    public StationNotFoundException(UUID stationId) {
        super("Station not found: " + stationId);
    }
}
