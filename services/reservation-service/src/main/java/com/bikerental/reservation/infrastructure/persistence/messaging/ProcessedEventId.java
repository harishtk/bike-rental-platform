package com.bikerental.reservation.infrastructure.persistence.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventId implements Serializable {

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "consumer_name")
    private String consumerName;
}
