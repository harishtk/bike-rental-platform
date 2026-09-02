package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetStationService implements GetStationUseCase {

    private final StationRepository stationRepository;

    public GetStationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }


    @Override
    public Station getById(UUID stationId) {
        return stationRepository.findById(stationId)
                .orElseThrow(() -> new StationNotFoundException(stationId));
    }
}
