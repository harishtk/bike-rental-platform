package com.bikerental.bike.infrastructure.persistence.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StationRepositoryAdapter implements StationRepository {

    private final SpringDataStationRepository repository;
    private final StationMapper mapper;

    public StationRepositoryAdapter(
            SpringDataStationRepository repository,
            StationMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Station save(Station station) {
        StationEntity entity = new StationEntity(
                station.getId(),
                station.getName(),
                station.getAddress(),
                station.getCapacity(),
                station.getStatus(),
                station.getCreatedAt(),
                station.getUpdatedAt()
        );

        StationEntity saved = repository.save(entity);

        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Station> findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public List<Station> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
