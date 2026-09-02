package com.bikerental.bike.domain.station;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StationRepository {

    Station save(Station station);

    Optional<Station> findById(UUID id);

    List<Station> findAll();
}
