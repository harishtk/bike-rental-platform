package com.bikerental.bike.api.station;

import com.bikerental.bike.application.station.CreateStationCommand;
import com.bikerental.bike.application.station.StationService;
import com.bikerental.bike.domain.station.Station;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final StationService stationService;
    private final StationResponseMapper stationResponseMapper;

    public StationController(StationService stationService, StationResponseMapper stationResponseMapper) {
        this.stationService = stationService;
        this.stationResponseMapper = stationResponseMapper;
    }

    @PostMapping
    public ResponseEntity<StationResponse> createStation(
            @Valid @RequestBody CreateStationRequest request,
            UriComponentsBuilder uriBuilder
    ) {
        CreateStationCommand command = new CreateStationCommand(
                request.name(),
                request.address(),
                request.capacity()
        );

        Station station = stationService.create(command);

        var location = uriBuilder.path("/api/v1/stations/{stationId}").buildAndExpand(station.getId()).toUri();
        return ResponseEntity.created(location).body(stationResponseMapper.toResponse(station));
    }

    @GetMapping("/{stationId}")
    public ResponseEntity<StationResponse> getStation(
            @PathVariable UUID stationId
    ) {
        Station station = stationService.getById(stationId);

        return ResponseEntity.ok(stationResponseMapper.toResponse(station));
    }

    @GetMapping
    public ResponseEntity<List<StationResponse>> getAllStations() {
        return ResponseEntity.ok(
                stationService.getAll()
                        .stream().map(stationResponseMapper::toResponse)
                        .toList()
        );
    }
}
