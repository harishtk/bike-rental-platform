package com.bikerental.reservation.infrastructure.outbox;

import com.bikerental.reservation.application.outbox.EventPayloadSerializer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class JacksonEventPayloadSerializer
    implements EventPayloadSerializer {

    private final ObjectMapper objectMapper;

    @Override
    public String serialize(Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new EventPayloadSerializationException(
                    "Failed to serialize event payload",
                    exception
            );
        }
    }
}
