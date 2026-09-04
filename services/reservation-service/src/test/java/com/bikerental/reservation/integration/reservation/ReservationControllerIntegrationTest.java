package com.bikerental.reservation.integration.reservation;

import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(
        scripts = "/sql/cleanup-reservations.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class ReservationControllerIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateStation() throws Exception {
        String request = """
                {
                    "userId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24",
                    "bikeId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc23",
                    "stationId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc25",
                    "durationHours": 48
                }
                """;

        mockMvc.perform(
                        post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(jsonPath(
                        "$.status",
                        is("ACTIVE")
                ));
    }

    @Test
    void shouldGetReservationById() throws Exception {
        String request = """
                {
                    "userId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24",
                    "bikeId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc23",
                    "stationId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc25",
                    "durationHours": 48
                }
                """;

        String response = mockMvc.perform(
                        post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String reservationId = read(response, "$.id");

        mockMvc.perform(
                        get("/api/v1/reservations/{reservationId}", reservationId)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id",
                        is(reservationId)
                ));
    }

    @Test
    void shouldReturn404WhenReservationDoesNotExist() throws Exception {
        mockMvc.perform(
                        get("/api/v1/reservations/{reservationId}", UUID.randomUUID())
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllReservations() throws Exception {
        UUID stationId = UUID.randomUUID();

        String id1 = createReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                stationId,
                48
        );
        String id2 = createReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                stationId,
                36
        );

        mockMvc.perform(
                get("/api/v1/reservations")
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", hasItems(id1, id2)));

    }

    @Test
    void shouldRejectInvalidStation() throws Exception {
        String request = """
                {
                    "userId": "",
                    "bikeId": "",
                    "stationId": "",
                    "durationHours": 0
                }
                """;
        mockMvc.perform(
                        post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.status",
                        is(400)
                ))
                .andExpect(jsonPath(
                        "$.data",
                        hasSize(4)
                ));

    }

    private String createReservation(
            UUID userId,
            UUID bikeId,
            UUID stationId,
            int durationHours
    ) throws Exception {
        String request = """
                {
                    "userId": "%s",
                    "bikeId": "%s",
                    "stationId": "%s",
                    "durationHours": %d
                }
                """.formatted(
                userId,
                bikeId,
                stationId,
                durationHours
        );

        String response = mockMvc.perform(
                        post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return read(response, "$.id");
    }
}
