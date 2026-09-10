package com.bikerental.reservation.infrastructure.outbox;

public class EventPayloadSerializationException extends RuntimeException {
    public EventPayloadSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
