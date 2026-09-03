package com.bikerental.bike.infrastructure.persistence.bike;

import com.bikerental.bike.domain.bike.Bike;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BikeMapper {

    BikeEntity toEntity(Bike bike);

    Bike toDomain(BikeEntity entity);
}
