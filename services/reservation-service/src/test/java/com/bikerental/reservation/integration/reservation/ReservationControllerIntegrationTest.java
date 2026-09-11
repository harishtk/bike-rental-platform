package com.bikerental.reservation.integration.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
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

    @MockitoBean
    private BikeReservationGateway bikeReservationGateway;

    @Test
    void shouldCreateReservation() throws Exception {
        UUID bikeId = prepareBikeReservationMock();

        String request = """
                {
                    "userId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24",
                    "bikeId": "%s",
                    "durationHours": 48
                }
                """.formatted(bikeId);

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
        UUID bikeId = prepareBikeReservationMock();
        String request = """
                {
                    "userId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24",
                    "bikeId": "%s",
                    "durationHours": 48
                }
                """.formatted(bikeId);

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
        when(bikeReservationGateway.reserveBike(ArgumentMatchers.any(UUID.class), ArgumentMatchers.any(UUID.class)))
            .thenReturn(new BikeReservationDetails(UUID.randomUUID(), UUID.randomUUID()));

        String id1 = createReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                48
        );
        String id2 = createReservation(
                UUID.randomUUID(),
                UUID.randomUUID(),
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
                        hasSize(3)
                ));

    }

    @Test
    void shouldCancelAValidReservation() throws Exception {
        UUID bikeId = prepareBikeReservationMock();

        doNothing().when(bikeReservationGateway).releaseBike(eq(bikeId), ArgumentMatchers.any(UUID.class));

        String reservationId = createReservation(
                UUID.randomUUID(),
                bikeId,
                48
        );

        mockMvc.perform(
                post("/api/v1/reservations/{reservationId}/cancel", reservationId)
        )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath(
                        "$.status",
                        is("CANCELLED")
                ));
    }

    @Test
    void shouldNotAllowTwoReservationsForSameUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bikeA = prepareBikeReservationMock();
        UUID bikeB = prepareBikeReservationMock();

        createReservation(userId, bikeA, 12);
        String request = """
                {
                    "userId": "%s",
                    "bikeId": "%s",
                    "durationHours": %d
                }
                """.formatted(
                userId,
                bikeB,
                12
        );

        mockMvc.perform(
                post("/api/v1/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isConflict());
    }

    private String createReservation(
            UUID userId,
            UUID bikeId,
            int durationHours
    ) throws Exception {
        String request = """
                {
                    "userId": "%s",
                    "bikeId": "%s",
                    "durationHours": %d
                }
                """.formatted(
                userId,
                bikeId,
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

    private UUID prepareBikeReservationMock() {
        UUID bikeId = UUID.randomUUID();
        BikeReservationDetails mockBikeReservationDetails =
                new BikeReservationDetails(
                        bikeId,
                        UUID.randomUUID()
                );

        when(bikeReservationGateway.reserveBike(eq(bikeId), ArgumentMatchers.any(UUID.class)))
                .thenReturn(mockBikeReservationDetails);
        return bikeId;
    }
}
