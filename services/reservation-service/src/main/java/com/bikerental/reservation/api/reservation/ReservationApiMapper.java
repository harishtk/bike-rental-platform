package com.bikerental.reservation.api.reservation;

import org.mapstruct.Mapper;

import com.bikerental.reservation.domain.reservation.Reservation;

@Mapper(componentModel = "spring")
public interface ReservationApiMapper {

    ReservationResponse toResponse(
            Reservation reservation
    );
}