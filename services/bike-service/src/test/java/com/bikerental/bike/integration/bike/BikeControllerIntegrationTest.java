package com.bikerental.bike.integration.bike;

import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static com.jayway.jsonpath.JsonPath.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(
        scripts = "/sql/cleanup-bikes.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class BikeControllerIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateBike() throws Exception {
        String request = """
                {
                    "serialNumber": "01",
                    "type": "Off-Road",
                    "stationId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24"
                }
                """;

        mockMvc.perform(
                post("/api/v1/bikes")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath(
                        "$.serialNumber",
                        is("01")
                ))
                .andExpect(jsonPath(
                        "$.stationId",
                        is("018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24")
                ))
                .andExpect(jsonPath(
                        "$.status",
                        is("AVAILABLE")
                ));
    }

    @Test
    void shouldGetBikeById() throws Exception {
        String request = """
                {
                    "serialNumber": "01",
                    "type": "Off-Road",
                    "stationId": "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24"
                }
                """;

        String response = mockMvc.perform(
                        post("/api/v1/bikes")
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bikeId = read(response, "$.id");

        mockMvc.perform(
                get("/api/v1/bikes/{bikeId}", bikeId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id",
                        is(bikeId)
                ))
                .andExpect(jsonPath(
                        "$.serialNumber",
                        is("01")
                ));
    }

    @Test
    void shouldReturn404WhenBikeDoesNotExist() throws Exception {
        mockMvc.perform(
                get("/api/v1/bikes/{bikeId}", "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24")
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllBikes() throws Exception {
        UUID stationId = UUID.randomUUID();
        createBike(
                "01",
                "type-1",
                stationId
        );

        createBike(
                "02",
                "type-2",
                stationId
        );

        mockMvc.perform(
                        get("/api/v1/bikes")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void shouldRejectInvalidBike() throws Exception {
        String request = """
                {
                    "serialNumber": "",
                    "type": "",
                    "stationId": ""
                }
                """;

        mockMvc.perform(
                post("/api/v1/bikes")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(
                        "$.status",
                        is(400)
                ))
                .andExpect(jsonPath(
                        "$.message",
                        is("Validation failed")
                ))
                .andExpect(jsonPath(
                        "$.data",
                        hasSize(3)
                ));
    }

    @Test
    void shouldRejectDuplicateSerialNumber() throws Exception {
        UUID stationId = UUID.randomUUID();

        String request = """
                {
                    "serialNumber": "01",
                    "type": "type-1",
                    "stationId": "%s"
                }
                """.formatted(stationId);

        mockMvc.perform(
                post("/api/v1/bikes")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/bikes")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isPreconditionFailed())
                .andExpect(jsonPath(
                        "$.code",
                        is("DUPLICATE_BIKE_SERIAL_NUMBER")
                ));
    }

    @Test
    void shouldGotoMaintenanceWhenMaintenanceCalled() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                post("/api/v1/bikes/{bikeId}/maintenance", bikeId)
        )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    void shouldBeAvailableWhenCompleteMaintenanceCalled() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/maintenance", bikeId)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/maintenance/complete", bikeId)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void shouldRetireWhenRetireCalled() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/retire", bikeId)
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("RETIRED")));
    }

    @Test
    void shouldReturn404WhenReservingNonExistentBike() throws Exception {
        mockMvc.perform(
                post("/api/v1/bikes/{bikeId}/reserve", UUID.randomUUID())
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenReservingAlreadyReservedBike() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/reserve", bikeId)
                )
                    .andExpect(status().isAccepted());

        mockMvc.perform(
                post("/api/v1/bikes/{bikeId}/reserve", bikeId)
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenReservingUnReservedBike() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/release", bikeId)
                )
                .andExpect(status().isConflict());
    }

    private String createBike(
            String serialNumber,
            String type,
            UUID stationId
    ) throws Exception {

        String request = """
                {
                    "serialNumber": "%s",
                    "type": "%s",
                    "stationId": "%s"
                }
                """.formatted(
                serialNumber,
                type,
                stationId
        );

        String response = mockMvc.perform(
                post("/api/v1/bikes")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return read(response, "$.id");
    }
}
