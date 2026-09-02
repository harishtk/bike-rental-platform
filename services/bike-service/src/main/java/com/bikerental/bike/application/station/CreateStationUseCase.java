package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;

public interface CreateStationUseCase {

    Station create(CreateStationCommand command);
}
