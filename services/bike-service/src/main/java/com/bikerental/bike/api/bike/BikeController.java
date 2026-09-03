package com.bikerental.bike.api.bike;

import com.bikerental.bike.api.common.PagedResponse;
import com.bikerental.bike.application.bike.BikeService;
import com.bikerental.bike.application.bike.CreateBikeCommand;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeFilter;
import com.bikerental.bike.domain.bike.BikeStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

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
    public ResponseEntity<PagedResponse<BikeResponse>> getBikes(
            @RequestParam(name = "stationId", required = false) UUID stationId,
            @RequestParam(name = "status", required = false) String status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        BikeStatus statusEnum = parseBikeStatus(status);
        BikeFilter filter = new BikeFilter(stationId, statusEnum, pageable);
        var pagedResponse   = PagedResponse.of(bikeService.getAll(filter, pageable)
                .map(mapper::toResponse));
        return ResponseEntity.ok(pagedResponse);
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

    private BikeStatus parseBikeStatus(String status) {
        try {
            if (status != null) {
                return BikeStatus.valueOf(status.toUpperCase());
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid status");
        }
        return null;
    }
}
