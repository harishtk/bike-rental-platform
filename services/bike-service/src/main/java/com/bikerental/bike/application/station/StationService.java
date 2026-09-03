package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class StationService {

    private final StationRepository stationRepository;

    public StationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    public Station create(CreateStationCommand command) {
        var station = Station.create(
                command.name(),
                command.address(),
                command.capacity()
        );
        return stationRepository.save(station);
    }

    public Station getById(UUID stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
    }

    public List<Station> getAll() {
        return stationRepository.findAll();
    }
}
