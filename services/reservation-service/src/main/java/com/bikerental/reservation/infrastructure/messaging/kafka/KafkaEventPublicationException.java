package com.bikerental.reservation.infrastructure.messaging.kafka;

public class KafkaEventPublicationException extends RuntimeException {
    public KafkaEventPublicationException(String message, Throwable cause) {
        super(message, cause);
    }
}
