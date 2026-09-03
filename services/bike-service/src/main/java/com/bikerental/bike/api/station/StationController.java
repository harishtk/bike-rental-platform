package com.bikerental.bike.api.station;

import com.bikerental.bike.application.station.CreateStationCommand;
import com.bikerental.bike.application.station.StationService;
import com.bikerental.bike.domain.station.Station;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(
            @Valid @RequestBody CreateStationRequest request
    ) {
        CreateStationCommand command = new CreateStationCommand(
                request.name(),
                request.address(),
                request.capacity()
        );

        Station station = stationService.create(command);

        return stationResponseMapper.toResponse(station);
    }

    @GetMapping("/{stationId}")
    public StationResponse getStation(
            @PathVariable UUID stationId
    ) {
        Station station = stationService.getById(stationId);

        return stationResponseMapper.toResponse(station);
    }

    @GetMapping
    public List<StationResponse> getAllStations() {
        return stationService.getAll()
                .stream().map(stationResponseMapper::toResponse)
                .toList();
    }
}
