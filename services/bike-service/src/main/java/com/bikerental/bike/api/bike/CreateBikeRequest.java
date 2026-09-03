package com.bikerental.bike.api.bike;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateBikeRequest(

        @NotBlank(message = "Bike serial number cannot be blank")
        @Size(max = 100, message = "Serial number cannot exceed 100 characters")
        String serialNumber,

        @NotBlank(message = "Bike type cannot be blank")
        @Size(max = 100, message = "Bike type cannot exceed 100 characters")
        String type,

        @NotNull(message = "Station ID is required")
        UUID stationId
) {
}
