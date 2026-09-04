package com.bikerental.reservation.infrastructure.persistence.reservation;

import org.mapstruct.Mapper;

import com.bikerental.reservation.domain.reservation.Reservation;
import org.springframework.stereotype.Component;

@Component
public class ReservationEntityMapper {

    public ReservationEntity toEntity(
            Reservation reservation
    ) {
        ReservationEntity entity =
                new ReservationEntity();

        entity.setId(reservation.getId());
        entity.setUserId(reservation.getUserId());
        entity.setBikeId(reservation.getBikeId());
        entity.setStationId(reservation.getStationId());
        entity.setReservedAt(reservation.getReservedAt());
        entity.setExpiresAt(reservation.getExpiresAt());
        entity.setStatus(reservation.getStatus());
        entity.setCancelledAt(reservation.getCancelledAt());
        entity.setCreatedAt(reservation.getCreatedAt());
        entity.setUpdatedAt(reservation.getUpdatedAt());

        return entity;
    }

    public Reservation toDomain(
            ReservationEntity entity
    ) {
        return Reservation.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getBikeId(),
                entity.getStationId(),
                entity.getReservedAt(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getCancelledAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}