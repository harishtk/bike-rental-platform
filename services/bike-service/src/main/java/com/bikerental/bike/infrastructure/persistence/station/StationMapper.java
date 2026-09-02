package com.bikerental.bike.infrastructure.persistence.station;

import com.bikerental.bike.domain.station.Station;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface StationMapper {

    StationEntity toEntity(Station station);

    Station toDomain(StationEntity entity);
}