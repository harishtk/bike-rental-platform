package com.bikerental.bike.integration.bike;

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
        scripts = "/sql/cleanup-bikes.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public class BikeControllerIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateBike() throws Exception {
        String request = """
                {
                    "serialNumber": "1212121212",
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
                        is("1212121212")
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
}
