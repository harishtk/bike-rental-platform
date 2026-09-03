package com.bikerental.bike.api.bike;

import com.bikerental.bike.application.bike.BikeService;
import com.bikerental.bike.application.bike.CreateBikeCommand;
import com.bikerental.bike.domain.bike.Bike;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

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

    @GetMapping("/{bikeId}")
    public BikeResponse getBike(
            @PathVariable UUID bikeId
    ) {
        Bike bike = bikeService.getById(bikeId);

        return mapper.toResponse(bike);
    }

    @GetMapping
    public List<BikeResponse> getBikes() {
        return bikeService.getAll()
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @PostMapping("/{bikeId}/reserve")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BikeResponse reserveBike(
            @PathVariable UUID bikeId
    ) {
        return mapper.toResponse(bikeService.reserve(bikeId));
    }

    @PostMapping("/{bikeId}/release")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BikeResponse releaseBike(
            @PathVariable UUID bikeId
    ) {
        return mapper.toResponse(bikeService.release(bikeId));
    }

    @PostMapping("/{bikeId}/maintenance")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BikeResponse startMaintenance(
            @PathVariable UUID bikeId
    ) {
        return mapper.toResponse(bikeService.startMaintenance(bikeId));
    }

    @PostMapping("/{bikeId}/maintenance/complete")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BikeResponse completeMaintenance(
            @PathVariable UUID bikeId
    ) {
        return mapper.toResponse(bikeService.completeMaintenance(bikeId));
    }

    @PostMapping("/{bikeId}/retire")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BikeResponse retireBike(
            @PathVariable UUID bikeId
    ) {
        return mapper.toResponse(bikeService.retire(bikeId));
    }
}
