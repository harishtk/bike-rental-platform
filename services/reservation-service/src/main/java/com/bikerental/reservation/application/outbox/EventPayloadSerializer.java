package com.bikerental.reservation.application.outbox;

public interface EventPayloadSerializer {

    String serialize(Object event);
}
