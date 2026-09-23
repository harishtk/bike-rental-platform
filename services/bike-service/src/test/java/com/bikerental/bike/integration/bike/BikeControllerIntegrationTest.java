package com.bikerental.bike.integration.bike;

import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@Sql(
        scripts = "/sql/cleanup-bikes.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class BikeControllerIntegrationTest extends
        AbstractPostgresIntegrationTest {

    private static RequestPostProcessor customer() {
        return jwt()
                .jwt(token -> token
                        .subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_USER"));
    }

    private static RequestPostProcessor admin() {
        return jwt()
                .jwt(token -> token
                        .subject(UUID.randomUUID().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    private static RequestPostProcessor service(
            String clientId,
            String scope
    ) {
        return jwt()
                .jwt(token -> token
                        .subject(clientId)
                        .claim("aud", List.of("bike-service"))
                        .claim("token_kind", "service")
                        .claim("scope", scope))
                .authorities(new SimpleGrantedAuthority("SCOPE_" + scope));
    }

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
                        .with(admin())
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
                                .with(admin())
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String bikeId = read(response, "$.id");

        mockMvc.perform(
                get("/api/v1/bikes/{bikeId}", bikeId)
                        .with(customer())
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
                        .with(customer())
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
                                .with(customer())
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
                        .with(admin())
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
                        .with(admin())
                        .contentType(APPLICATION_JSON)
                        .with(admin())
                        .content(request)
        )
                .andExpect(status().isCreated());

        mockMvc.perform(
                post("/api/v1/bikes")
                        .with(admin())
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
                        .with(admin())
        )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));
    }

    @Test
    void shouldBeAvailableWhenCompleteMaintenanceCalled() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/maintenance", bikeId)
                                .with(admin())
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("MAINTENANCE")));

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/maintenance/complete", bikeId)
                                .with(admin())
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("AVAILABLE")));
    }

    @Test
    void shouldRetireWhenRetireCalled() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/retire", bikeId)
                            .with(admin())
                )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status", is("RETIRED")));
    }

    @Test
    void shouldReturn404WhenReservingNonExistentBike() throws Exception {
        mockMvc.perform(
                post("/api/v1/bikes/{bikeId}/reserve", UUID.randomUUID())
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .with(service("reservation-service", "bike:reservation"))
        )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenReservingAlreadyReservedBike() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/reserve", bikeId)
                                .header("X-Idempotency-Key", UUID.randomUUID())
                                .with(service("reservation-service", "bike:reservation"))
                )
                    .andExpect(status().isAccepted());

        mockMvc.perform(
                post("/api/v1/bikes/{bikeId}/reserve", bikeId)
                        .header("X-Idempotency-Key", UUID.randomUUID())
                        .with(service("reservation-service", "bike:reservation"))
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenReservingUnReservedBike() throws Exception {
        String bikeId = createBike("01", "Off-Road", UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/release", bikeId)
                                .header("X-Idempotency-Key", UUID.randomUUID())
                                .with(service("reservation-service", "bike:reservation"))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRentBike() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/bikes/{bikeId}", bikeId)
                                .with(customer())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.status",
                        is("RENTED")
                ));
    }

    @Test
    void shouldReturnRentedBike() throws Exception {
        UUID originalStationId = UUID.randomUUID();
        UUID returnStationId = UUID.randomUUID();

        String bikeId = createBike(
                "01",
                "Off-Road",
                originalStationId
        );

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(returnStationId);

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/bikes/{bikeId}", bikeId)
                                .with(customer())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.status",
                        is("AVAILABLE")
                ))
                .andExpect(jsonPath(
                        "$.stationId",
                        is(returnStationId.toString())
                ));
    }

    @Test
    void shouldReturn404WhenRentingNonExistentBike() throws Exception {
        mockMvc.perform(
                        post(
                                "/api/v1/bikes/{bikeId}/rent",
                                UUID.randomUUID()
                        )
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn404WhenReturningNonExistentBike() throws Exception {
        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post(
                                "/api/v1/bikes/{bikeId}/return",
                                UUID.randomUUID()
                        )
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn409WhenRentingAlreadyRentedBike() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenReturningBikeThatIsNotRented() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRequireIdempotencyKeyWhenRentingBike() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireIdempotencyKeyWhenReturningBike() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRentBikeIdempotently() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        UUID operationId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/bikes/{bikeId}", bikeId)
                                .with(customer())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.status",
                        is("RENTED")
                ));
    }

    @Test
    void shouldReturnBikeIdempotently() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        UUID.randomUUID()
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        UUID returnStationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(returnStationId);

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        get("/api/v1/bikes/{bikeId}", bikeId)
                                .with(customer())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.status",
                        is("AVAILABLE")
                ))
                .andExpect(jsonPath(
                        "$.stationId",
                        is(returnStationId.toString())
                ));
    }

    @Test
    void shouldReturn409WhenSameIdempotencyKeyUsedForRentAndReturn() throws Exception {
        String bikeId = createBike(
                "01",
                "Off-Road",
                UUID.randomUUID()
        );

        UUID operationId = UUID.randomUUID();

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/rent", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                )
                .andExpect(status().isNoContent());

        String request = """
            {
                "stationId": "%s"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(
                        post("/api/v1/bikes/{bikeId}/return", bikeId)
                                .header(
                                        "X-Idempotency-Key",
                                        operationId
                                )
                                .with(service("rental-service", "bike:rental"))
                                .contentType(APPLICATION_JSON)
                                .content(request)
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
                        .with(admin())
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
