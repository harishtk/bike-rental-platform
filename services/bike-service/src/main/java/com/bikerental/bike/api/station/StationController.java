package com.bikerental.bike.api.station;

import com.bikerental.bike.api.common.ErrorResponse;
import com.bikerental.bike.application.station.CreateStationCommand;
import com.bikerental.bike.application.station.CreateStationUseCase;
import com.bikerental.bike.application.station.GetStationUseCase;
import com.bikerental.bike.application.station.ListStationUseCase;
import com.bikerental.bike.domain.station.Station;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stations")
public class StationController {

    private final CreateStationUseCase createStationUseCase;
    private final GetStationUseCase getStationUseCase;
    private final ListStationUseCase listStationUseCase;
    private final StationResponseMapper stationResponseMapper;

    public StationController(CreateStationUseCase createStationUseCase, GetStationUseCase getStationUseCase, ListStationUseCase listStationUseCase, StationResponseMapper stationResponseMapper) {
        this.createStationUseCase = createStationUseCase;
        this.getStationUseCase = getStationUseCase;
        this.listStationUseCase = listStationUseCase;
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

        Station station = createStationUseCase.create(command);

        return stationResponseMapper.toResponse(station);
    }

    @GetMapping("/{stationId}")
    public StationResponse getStation(
            @PathVariable UUID stationId
    ) {
        Station station = getStationUseCase.getById(stationId);

        return stationResponseMapper.toResponse(station);
    }

    @GetMapping
    public List<StationResponse> getAllStations() {
        return listStationUseCase.getAll()
                .stream().map(stationResponseMapper::toResponse)
                .toList();
    }
}
