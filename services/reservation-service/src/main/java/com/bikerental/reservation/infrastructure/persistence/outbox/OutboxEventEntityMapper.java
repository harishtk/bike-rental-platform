package com.bikerental.reservation.infrastructure.persistence.outbox;

import com.bikerental.reservation.domain.outbox.OutboxEvent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OutboxEventEntityMapper {

    OutboxEventEntity toEntity(OutboxEvent event);

    OutboxEvent toDomain(OutboxEventEntity entity);
}
