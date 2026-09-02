package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;

import java.util.UUID;

public interface GetStationUseCase {

    Station getById(UUID stationId);
}