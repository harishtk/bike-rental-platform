package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListStationService implements ListStationUseCase {

    private final StationRepository stationRepository;

    public ListStationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public List<Station> getAll() {
        return stationRepository.findAll();
    }
}
