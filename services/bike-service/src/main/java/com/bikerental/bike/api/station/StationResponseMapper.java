package com.bikerental.bike.api.station;

import com.bikerental.bike.domain.station.Station;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StationResponseMapper {

    StationResponse toResponse(Station station);
}
