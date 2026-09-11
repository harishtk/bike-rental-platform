package com.bikerental.reservation.infrastructure.messaging.kafka;

import com.bikerental.reservation.domain.outbox.OutboxEvent;
import com.bikerental.reservation.application.outbox.OutboxEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Component
public class KafkaOutboxEventPublisher
        implements OutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${kafka.topics.reservation-events}")
    private String topic;

    @Override
    public void publish(OutboxEvent event) {
        try {
            kafkaTemplate
                    .send(
                        topic,
                        event.getAggregateId().toString(),
                        event.getPayload()
                    )
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new KafkaEventPublicationException(
                    "Failed to publish outbox event: " + event.getId(),
                    e
            );
        }
    }
}
