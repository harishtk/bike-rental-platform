package com.bikerental.bike.application.station;

import com.bikerental.bike.domain.station.Station;

import java.util.List;

public interface ListStationUseCase {

    List<Station> getAll();
}
