package com.bikerental.rental.api.rental;

import com.bikerental.rental.domain.rental.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RentalResponseMapper {

    RentalResponse toResponse(Rental rental);
}
