package com.bikerental.bike.api.bike;

import com.bikerental.bike.application.bike.BikeService;
import com.bikerental.bike.application.bike.CreateBikeCommand;
import com.bikerental.bike.domain.bike.Bike;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

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
    public ResponseEntity<BikeResponse> create(
            @Valid @RequestBody CreateBikeRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        Bike bike = bikeService.create(
                new CreateBikeCommand(
                        request.serialNumber(),
                        request.type(),
                        request.stationId()
                )
        );

        var location = uriBuilder.path("/api/v1/bikes/{bikeId}").buildAndExpand(bike.getId()).toUri();
        return ResponseEntity.created(location).body(mapper.toResponse(bike));
    }

    @GetMapping("/{bikeId}")
    public ResponseEntity<BikeResponse> getBike(
            @PathVariable UUID bikeId
    ) {
        Bike bike = bikeService.getById(bikeId);

        return ResponseEntity.ok(mapper.toResponse(bike));
    }

    @GetMapping
    public ResponseEntity<List<BikeResponse>> getBikes(
            @RequestParam(name = "stationId", required = false) UUID stationId,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "10") int pageSize
    ) {
        Pageable pageable = PageRequest.of(page, pageSize);

        return ResponseEntity.ok(
                bikeService.getAll(stationId, status, pageable)
                        .stream()
                        .map(mapper::toResponse)
                        .toList()
        );
    }

    @PostMapping("/{bikeId}/reserve")
    public ResponseEntity<BikeResponse> reserveBike(
            @PathVariable UUID bikeId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(bikeService.reserve(bikeId)));
    }

    @PostMapping("/{bikeId}/release")
    public ResponseEntity<BikeResponse> releaseBike(
            @PathVariable UUID bikeId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(bikeService.release(bikeId)));
    }

    @PostMapping("/{bikeId}/maintenance")
    public ResponseEntity<BikeResponse> startMaintenance(
            @PathVariable UUID bikeId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(bikeService.startMaintenance(bikeId)));
    }

    @PostMapping("/{bikeId}/maintenance/complete")
    public ResponseEntity<BikeResponse> completeMaintenance(
            @PathVariable UUID bikeId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(bikeService.completeMaintenance(bikeId)));
    }

    @PostMapping("/{bikeId}/retire")
    public ResponseEntity<BikeResponse> retireBike(
            @PathVariable UUID bikeId
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(mapper.toResponse(bikeService.retire(bikeId)));
    }
}
