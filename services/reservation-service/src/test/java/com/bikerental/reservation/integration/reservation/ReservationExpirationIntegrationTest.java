package com.bikerental.reservation.integration.reservation;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import com.bikerental.reservation.ReservationServiceApplication;
import com.bikerental.reservation.application.reservation.ReservationExpirationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.github.tomakehurst.wiremock.WireMockServer;

import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.infrastructure.persistence.reservation.SpringDataReservationRepository;

@Testcontainers
@SpringBootTest(classes = {ReservationServiceApplication.class})
class ReservationExpirationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("reservation_db")
                    .withUsername("reservation")
                    .withPassword("reservation");

    private static final WireMockServer wireMockServer =
            new WireMockServer(8089);

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SpringDataReservationRepository springDataReservationRepository;

    @Autowired
    private ReservationExpirationService expirationService;

    private UUID reservationId;
    private UUID userId;
    private UUID bikeId;
    private UUID stationId;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "clients.bike-service.url",
                () -> "http://localhost:" + wireMockServer.port()
        );

        registry.add(
                "reservation.expiration.fixed-delay",
                () -> "3600000"
        );
    }

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @BeforeEach
    void setUp() {

        if (!wireMockServer.isRunning()) {
            wireMockServer.start();
        }

        wireMockServer.resetAll();

        springDataReservationRepository.deleteAll();

        reservationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        bikeId = UUID.randomUUID();
        stationId = UUID.randomUUID();
    }

    @Test
    void shouldExpireReservationAndReleaseBike() {

        Instant reservedAt =
                Instant.parse("2026-01-01T10:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-01-02T10:00:00Z");

        Reservation reservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        reservationRepository.save(reservation);

        wireMockServer.stubFor(
                post(urlEqualTo(
                        "/api/v1/bikes/" + bikeId + "/release"
                ))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                )
        );

        /*
         * The service internally uses Instant.now().
         *
         * Therefore the reservation above must already be expired
         * relative to the real current time.
         */
        expirationService.expireReservations();

        Reservation persisted =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        wireMockServer.verify(
                1,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );
    }

    @Test
    void shouldNotExpireFutureReservation() {

        Instant reservedAt =
                Instant.now().minusSeconds(60);

        Instant expiresAt =
                Instant.now().plusSeconds(3600);

        Reservation reservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        reservationRepository.save(reservation);

        expirationService.expireReservations();

        Reservation persisted =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(ReservationStatus.ACTIVE);

        wireMockServer.verify(
                0,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );
    }

    @Test
    void shouldNotExpireCancelledReservation() {

        Instant reservedAt =
                Instant.parse("2026-01-01T10:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-01-02T10:00:00Z");

        Instant cancelledAt =
                Instant.parse("2026-01-01T12:00:00Z");

        Reservation reservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.CANCELLED,
                cancelledAt,
                reservedAt,
                cancelledAt
        );

        reservationRepository.save(reservation);

        expirationService.expireReservations();

        Reservation persisted =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(ReservationStatus.CANCELLED);

        wireMockServer.verify(
                0,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );
    }

    @Test
    void shouldNotExpireConsumedReservation() {

        Instant reservedAt =
                Instant.parse("2026-01-01T10:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-01-02T10:00:00Z");

        Reservation reservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.CONSUMED,
                null,
                reservedAt,
                reservedAt
        );

        reservationRepository.save(reservation);

        expirationService.expireReservations();

        Reservation persisted =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        assertThat(persisted.getStatus())
                .isEqualTo(ReservationStatus.CONSUMED);

        wireMockServer.verify(
                0,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );
    }

    @Test
    void shouldKeepReservationActiveWhenBikeReleaseFails() {

        Instant reservedAt =
                Instant.parse("2026-01-01T10:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-01-02T10:00:00Z");

        Reservation reservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        reservationRepository.save(reservation);

        wireMockServer.stubFor(
                post(urlEqualTo(
                        "/api/v1/bikes/" + bikeId + "/release"
                ))
                .willReturn(
                        aResponse()
                                .withStatus(500)
                )
        );

        expirationService.expireReservations();

        Reservation persisted =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        /*
         * Critical invariant:
         *
         * Bike release failed
         *        ↓
         * Reservation must remain ACTIVE
         *
         * This allows the scheduler to retry later.
         */
        assertThat(persisted.getStatus())
                .isEqualTo(ReservationStatus.ACTIVE);

        wireMockServer.verify(
                1,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );
    }

    @Test
    void shouldExpireMultipleReservations() {

        UUID secondReservationId = UUID.randomUUID();
        UUID secondBikeId = UUID.randomUUID();

        Instant reservedAt =
                Instant.parse("2026-01-01T10:00:00Z");

        Instant expiresAt =
                Instant.parse("2026-01-02T10:00:00Z");

        Reservation firstReservation = Reservation.restore(
                reservationId,
                userId,
                bikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        Reservation secondReservation = Reservation.restore(
                secondReservationId,
                UUID.randomUUID(),
                secondBikeId,
                stationId,
                reservedAt,
                expiresAt,
                ReservationStatus.ACTIVE,
                null,
                reservedAt,
                reservedAt
        );

        reservationRepository.save(firstReservation);
        reservationRepository.save(secondReservation);

        wireMockServer.stubFor(
                post(urlEqualTo(
                        "/api/v1/bikes/" + bikeId + "/release"
                ))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                )
        );

        wireMockServer.stubFor(
                post(urlEqualTo(
                        "/api/v1/bikes/" + secondBikeId + "/release"
                ))
                .willReturn(
                        aResponse()
                                .withStatus(200)
                )
        );

        expirationService.expireReservations();

        Reservation persistedFirst =
                reservationRepository
                        .findById(reservationId)
                        .orElseThrow();

        Reservation persistedSecond =
                reservationRepository
                        .findById(secondReservationId)
                        .orElseThrow();

        assertThat(persistedFirst.getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        assertThat(persistedSecond.getStatus())
                .isEqualTo(ReservationStatus.EXPIRED);

        wireMockServer.verify(
                1,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + bikeId + "/release"
                        )
                )
        );

        wireMockServer.verify(
                1,
                postRequestedFor(
                        urlEqualTo(
                                "/api/v1/bikes/" + secondBikeId + "/release"
                        )
                )
        );
    }
}