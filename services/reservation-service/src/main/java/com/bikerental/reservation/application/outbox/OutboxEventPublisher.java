package com.bikerental.reservation.application.outbox;

import com.bikerental.reservation.domain.outbox.OutboxEvent;

public interface OutboxEventPublisher {

    void publish(OutboxEvent event);
}
