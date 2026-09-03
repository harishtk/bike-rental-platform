package com.bikerental.bike.api.bike;

import com.bikerental.bike.domain.bike.Bike;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BikeResponseMapper {

    BikeResponse toResponse(Bike bike);
}
