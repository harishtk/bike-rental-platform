package com.bikerental.bike.api.bike;

import com.bikerental.bike.application.bike.BikeService;
import com.bikerental.bike.application.bike.CreateBikeCommand;
import com.bikerental.bike.domain.bike.Bike;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bikes")
public class BikeController {

    private final BikeService bikeService;
    private final BikeResponseMapper mapper;

    public BikeController(BikeService bikeService, BikeResponseMapper mapper) {
        this.bikeService = bikeService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BikeResponse create(
            @Valid @RequestBody CreateBikeRequest request
    ) {
        Bike bike = bikeService.create(
                new CreateBikeCommand(
                        request.serialNumber(),
                        request.type(),
                        request.stationId()
                )
        );

        return mapper.toResponse(bike);
    }
}
