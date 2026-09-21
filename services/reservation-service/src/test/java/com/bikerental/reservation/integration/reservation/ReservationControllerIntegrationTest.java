package com.bikerental.reservation.integration.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.infrastructure.persistence.outbox.SpringDataOutboxEventRepository;
import static org.assertj.core.api.Assertions.assertThat;

import static com.jayway.jsonpath.JsonPath.read;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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

    private static RequestPostProcessor asCustomer(UUID userId) {
        return jwt().jwt(token -> token.subject(userId.toString()));
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SpringDataOutboxEventRepository outboxRepository;

    @MockitoBean
    private BikeReservationGateway bikeReservationGateway;

    @Test
    void shouldCreateReservation() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bikeId = prepareBikeReservationMock();

        String request = """
                {
                    "bikeId": "%s",
                    "durationHours": 48
                }
                """.formatted(bikeId);

        mockMvc.perform(
                        post("/api/v1/reservations")
                                .with(asCustomer(userId))
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
        UUID userId = UUID.randomUUID();
        UUID bikeId = prepareBikeReservationMock();
        String request = """
                {
                    "bikeId": "%s",
                    "durationHours": 48
                }
                """.formatted(bikeId);

        String response = mockMvc.perform(
                        post("/api/v1/reservations")
                                .with(asCustomer(userId))
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
                                .with(asCustomer(userId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath(
                        "$.id",
                        is(reservationId)
                ));
    }

    @Test
    void shouldReturn404WhenReservationDoesNotExist() throws Exception {
        UUID userId = UUID.randomUUID();
        mockMvc.perform(
                        get("/api/v1/reservations/{reservationId}", UUID.randomUUID())
                                .with(asCustomer(userId))
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldGetOnlyOwnedReservations() throws Exception {
        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();
        when(bikeReservationGateway.reserveBike(ArgumentMatchers.any(UUID.class), ArgumentMatchers.any(UUID.class)))
            .thenReturn(new BikeReservationDetails(UUID.randomUUID(), UUID.randomUUID()));

        String id1 = createReservation(
                firstUserId,
                UUID.randomUUID(),
                48
        );
        String id2 = createReservation(
                secondUserId,
                UUID.randomUUID(),
                36
        );

        mockMvc.perform(
                get("/api/v1/reservations")
                        .with(asCustomer(firstUserId))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].id", hasItems(id1)));

        mockMvc.perform(
                        get("/api/v1/reservations")
                                .with(asCustomer(secondUserId))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[*].id", hasItems(id2)));

    }

    @Test
    void shouldRejectInvalidReservationRequest() throws Exception {
        UUID userId = UUID.randomUUID();
        String request = """
                {
                    "bikeId": "",
                    "durationHours": 0
                }
                """;
        mockMvc.perform(
                        post("/api/v1/reservations")
                                .with(asCustomer(userId))
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
                        hasSize(2)
                ));

    }

    @Test
    void shouldCancelAValidReservation() throws Exception {
        UUID userId =  UUID.randomUUID();
        UUID bikeId = prepareBikeReservationMock();

        doNothing().when(bikeReservationGateway).releaseBike(eq(bikeId), ArgumentMatchers.any(UUID.class));

        String reservationId = createReservation(
                userId,
                bikeId,
                48
        );

        mockMvc.perform(
                post("/api/v1/reservations/{reservationId}/cancel", reservationId)
                        .with(asCustomer(userId))
        )
                .andExpect(status().isAccepted())
                .andExpect(jsonPath(
                        "$.status",
                        is("CANCELLED")
                ));
    }

    @Test
    void shouldRejectCreationWithoutToken() throws Exception {
        mockMvc.perform(
                        post("/api/v1/reservations")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "bikeId": "%s",
                                        "durationHours": 48
                                    }
                                    """.formatted(UUID.randomUUID()))
                )
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bikeReservationGateway);
    }

    @Test
    void shouldUseTokenOwnerInsteadOfBodyUserId() throws Exception {
        UUID authenticatedUser = UUID.randomUUID();
        UUID anotherUser = UUID.randomUUID();
        UUID bikeId = prepareBikeReservationMock();

        mockMvc.perform(
                        post("/api/v1/reservations")
                                .with(asCustomer(authenticatedUser))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                        "userId": "%s",
                                        "bikeId": "%s",
                                        "durationHours": 48
                                    }
                                    """.formatted(anotherUser, bikeId))
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId")
                        .value(authenticatedUser.toString()));
    }

    @Test
    void shouldNotAllowTwoReservationsForSameUser() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID bikeA = prepareBikeReservationMock();
        UUID bikeB = prepareBikeReservationMock();

        createReservation(userId, bikeA, 12);
        String request = """
                {
                    "bikeId": "%s",
                    "durationHours": %d
                }
                """.formatted(
                bikeB,
                12
        );

        mockMvc.perform(
                post("/api/v1/reservations")
                        .with(asCustomer(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)
        )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnEmptyListForCustomerWithoutReservations() throws Exception {
        createReservation(UUID.randomUUID(), prepareBikeReservationMock(), 48);

        mockMvc.perform(get("/api/v1/reservations")
                        .with(asCustomer(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldHideAnotherCustomersReservation() throws Exception {
        UUID owner = UUID.randomUUID();
        String id = createReservation(owner, prepareBikeReservationMock(), 48);

        mockMvc.perform(get("/api/v1/reservations/{id}", id)
                        .with(asCustomer(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/reservations/{id}", id)
                        .with(asCustomer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(owner.toString()));
    }

    @Test
    void shouldRejectForeignCancellationWithoutSideEffects() throws Exception {
        UUID owner = UUID.randomUUID();
        String id = createReservation(owner, prepareBikeReservationMock(), 48);
        long eventCount = outboxRepository.count();
        clearInvocations(bikeReservationGateway);

        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", id)
                        .with(asCustomer(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        verifyNoInteractions(bikeReservationGateway);
        assertThat(reservationRepository.findByIdAndUserId(UUID.fromString(id), owner)
                .orElseThrow().getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(outboxRepository.count()).isEqualTo(eventCount);
    }

    @Test
    void shouldRejectMissingCancellationWithoutSideEffects() throws Exception {
        long eventCount = outboxRepository.count();
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", UUID.randomUUID())
                        .with(asCustomer(UUID.randomUUID())))
                .andExpect(status().isNotFound());
        verifyNoInteractions(bikeReservationGateway);
        assertThat(outboxRepository.count()).isEqualTo(eventCount);
    }

    @Test
    void shouldRequireAuthenticationForReadsAndCancellation() throws Exception {
        String id = createReservation(UUID.randomUUID(), prepareBikeReservationMock(), 48);
        clearInvocations(bikeReservationGateway);
        mockMvc.perform(get("/api/v1/reservations")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reservations/{id}", id))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/reservations/{id}/cancel", id))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(bikeReservationGateway);
    }

    private String createReservation(
            UUID userId,
            UUID bikeId,
            int durationHours
    ) throws Exception {
        String request = """
                {
                    "bikeId": "%s",
                    "durationHours": %d
                }
                """.formatted(
                bikeId,
                durationHours
        );

        String response = mockMvc.perform(
                        post("/api/v1/reservations")
                                .with(asCustomer(userId))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists("location"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
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
