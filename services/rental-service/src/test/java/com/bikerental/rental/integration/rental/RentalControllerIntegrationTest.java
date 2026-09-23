package com.bikerental.rental.integration.rental;

import com.bikerental.rental.domain.rental.RentalRepository;
import com.bikerental.rental.domain.rental.RentalStatus;
import com.bikerental.rental.integration.AbstractPostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import static com.jayway.jsonpath.JsonPath.read;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Sql(scripts = "/sql/cleanup-rentals.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RentalControllerIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");

    private static RequestPostProcessor asCustomer(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RentalRepository rentalRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setTime() {
        when(clock.instant()).thenReturn(START);
    }

    @Test
    void shouldCreateRental() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();

        String id = createRental(userId, bikeId, stationId);

        var saved = rentalRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getBikeId()).isEqualTo(bikeId);
        assertThat(saved.getStartStationId()).isEqualTo(stationId);
        assertThat(saved.getStartedAt()).isEqualTo(START);
        assertThat(saved.getDailyRate()).isEqualByComparingTo("25.00");
        assertThat(saved.getVersion()).isZero();
        verify(bikeRentalGateway).startRental(eq(bikeId), any(UUID.class));
    }

    @Test
    void shouldReturnAndCompleteRentalAcrossRequests() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bikeId = UUID.randomUUID();
        String id = createRental(userId, bikeId, UUID.randomUUID());
        verify(bikeRentalGateway).startRental(eq(bikeId), any(UUID.class));
        UUID returnStation = UUID.randomUUID();
        Instant returnedAt = START.plusSeconds(3600);
        when(clock.instant()).thenReturn(returnedAt);

        mockMvc.perform(post("/api/v1/rentals/{id}/return", id)
                        .contentType(APPLICATION_JSON)
                        .with(asCustomer(userId))
                        .content("""
                                {"stationId": "%s"}
                                """.formatted(returnStation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("RETURNED"))
                .andExpect(jsonPath("$.returnStationId").value(returnStation.toString()))
                .andExpect(jsonPath("$.returnedAt").value(returnedAt.toString()))
                .andExpect(jsonPath("$.totalAmount").value(25.0));

        assertThat(rentalRepository.findById(UUID.fromString(id)).orElseThrow().getVersion()).isEqualTo(1L);
        verify(bikeRentalGateway).returnBike(eq(bikeId), eq(returnStation), any(UUID.class));

        // The complete endpoint is private until a trusted payment service is implemented
        mockMvc.perform(
                    post("/api/v1/rentals/{id}/complete", id)
                            .with(asCustomer(userId))
                )
                .andExpect(status().isForbidden());

        var completed = rentalRepository.findById(UUID.fromString(id)).orElseThrow();
        assertThat(completed.getStatus()).isEqualTo(RentalStatus.RETURNED);
        // assertThat(completed.getVersion()).isEqualTo(2L);
        verifyNoMoreInteractions(bikeRentalGateway);
    }

    @ParameterizedTest
    @ValueSource(strings = {"bikeId", "stationId", "dailyRate"})
    void shouldRejectMissingRequiredCreateField(String field) throws Exception {
        UUID userId = UUID.randomUUID();
        var body = new HashMap<String, Object>();
        body.put("bikeId", UUID.randomUUID().toString());
        body.put("stationId", UUID.randomUUID().toString());
        body.put("dailyRate", 25);
        body.remove(field);

        mockMvc.perform(post("/api/v1/rentals")
                        .contentType(APPLICATION_JSON)
                        .with(asCustomer(userId))
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bikeRentalGateway);
    }

    @Test
    void shouldRejectMissingReturnStation() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/rentals/{id}/return", UUID.randomUUID())
                        .with(asCustomer(userId))
                        .contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bikeRentalGateway);
    }

    @Test
    void shouldRejectMalformedRentalId() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/rentals/{id}/complete", "invalid-id")
                        .with(asCustomer(userId)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bikeRentalGateway);
    }

    private String createRental(UUID userId, UUID bikeId, UUID stationId) throws Exception {
        String response = mockMvc.perform(post("/api/v1/rentals")
                        .contentType(APPLICATION_JSON)
                        .with(asCustomer(userId))
                        .content("""
                                {"userId": "%s", "bikeId": "%s", "stationId": "%s", "dailyRate": 25.00}
                                """.formatted(userId, bikeId, stationId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.bikeId").value(bikeId.toString()))
                .andExpect(jsonPath("$.startStationId").value(stationId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.totalAmount").value(0.0))
                .andReturn().getResponse().getContentAsString();
        return read(response, "$.id");
    }
}
