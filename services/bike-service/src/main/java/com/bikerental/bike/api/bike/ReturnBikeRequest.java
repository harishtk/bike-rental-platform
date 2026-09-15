package com.bikerental.bike.api.bike;

import java.util.UUID;

public record ReturnBikeRequest(
        UUID stationId
) {
}
