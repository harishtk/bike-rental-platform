package com.bikerental.bike.application.station;

public record CreateStationCommand(
        String name,
        String address,
        int capacity
) {
}
