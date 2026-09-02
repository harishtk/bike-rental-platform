package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class CreateStationService implements CreateStationUseCase {

    private final StationRepository stationRepository;

    public CreateStationService(StationRepository stationRepository) {
        this.stationRepository = stationRepository;
    }

    @Override
    public Station create(CreateStationCommand command) {
        var station = Station.create(
                command.name(),
                command.address(),
                command.capacity()
        );
        return stationRepository.save(station);
    }
}
