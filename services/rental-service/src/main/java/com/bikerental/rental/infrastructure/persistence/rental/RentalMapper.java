package com.bikerental.rental.infrastructure.persistence.rental;

import com.bikerental.rental.domain.rental.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RentalMapper {

    Rental toDomain(RentalEntity rentalEntity);

    RentalEntity toEntity(Rental rental);
}
