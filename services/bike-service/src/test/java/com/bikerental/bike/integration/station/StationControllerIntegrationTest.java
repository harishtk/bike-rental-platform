package com.bikerental.bike.integration.station;

import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static com.jayway.jsonpath.JsonPath.*;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(
        scripts = "/sql/cleanup-stations.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class StationControllerIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateStation() throws Exception {
        String request = """
                {
                    "name": "Central Station",
                    "address": "12 MG Road, Bengaluru",
                    "capacity": 25
                }
                """;

        mockMvc.perform(
                post("/api/v1/stations")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated())
                .andExpect(jsonPath(
                        "$.name",
                        is("Central Station")
                ))
                .andExpect(jsonPath(
                        "$.address",
                        is("12 MG Road, Bengaluru")
                ))
                .andExpect(jsonPath(
                        "$.capacity",
                        is(25)
                ))
                .andExpect(jsonPath(
                        "$.status",
                        is("ACTIVE")
                ));
    }

    @Test
    void shouldGetStationById() throws Exception {
        String request = """
                {
                    "name": "Central Station",
                    "address": "12 MG Road, Bengaluru",
                    "capacity": 25
                }
                """;

        String response = mockMvc.perform(
                post("/api/v1/stations")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String stationId = read(response, "$.id");

        mockMvc.perform(
                get("/api/v1/stations/{stationId}", stationId)
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id",
                        is(stationId)
                ))
                .andExpect(jsonPath(
                        "$.name",
                        is("Central Station")
                ));
    }

    @Test
    void shouldReturn404WhenStationDoesNotExist() throws Exception {
        mockMvc.perform(
                get("/api/v1/stations/{stationId}", "018f9dd7-7d9a-7f85-ae7c-5c97e2c5dc24")
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetAllStations() throws Exception {
        createStation(
                "Central Station",
                "MG Road, Bengaluru",
                25
        );

        createStation(
                "North Station",
                "Hebbal, Bengaluru",
                15
        );

        mockMvc.perform(
                        get("/api/v1/stations")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldRejectInvalidStation() throws Exception {
        String request = """
                {
                    "name": "",
                    "address": "",
                    "capacity": 0
                }
                """;

        mockMvc.perform(
                        post("/api/v1/stations")
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

    private void createStation(
            String name,
            String address,
            int capacity
    ) throws Exception {

        String request = """
                {
                    "name": "%s",
                    "address": "%s",
                    "capacity": %d
                }
                """.formatted(
                name,
                address,
                capacity
        );

        mockMvc.perform(
                post("/api/v1/stations")
                        .contentType(APPLICATION_JSON)
                        .content(request)
        ).andExpect(status().isCreated());
    }
}
