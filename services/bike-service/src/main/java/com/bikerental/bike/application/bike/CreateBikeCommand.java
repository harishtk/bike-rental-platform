package com.bikerental.bike.application.bike;

import java.util.UUID;

public record CreateBikeCommand(
        String serialNumber,
        String type,
        UUID stationId
) {
}
