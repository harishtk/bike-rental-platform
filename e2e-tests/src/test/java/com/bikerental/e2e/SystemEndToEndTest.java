package com.bikerental.e2e;

import io.restassured.http.ContentType;
import io.restassured.filter.Filter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class SystemEndToEndTest {

    private static final Logger log = LoggerFactory.getLogger(SystemEndToEndTest.class);

    private static final Filter HTTP_LOGGING = (request, responseSpec, context) -> {
        long started = System.nanoTime();
        log.debug("Sending {} {}", request.getMethod(), request.getUserDefinedPath());
        var response = context.next(request, responseSpec);
        log.debug("Received {} {}: status={}, elapsedMs={}",
                request.getMethod(), request.getUserDefinedPath(), response.statusCode(),
                (System.nanoTime() - started) / 1_000_000);
        return response;
    };

    private static String gatewayUrl;

    @BeforeAll
    static void setup() {
        gatewayUrl = System.getenv()
                .getOrDefault(
                        "API_GATEWAY_URL",
                        "http://localhost:8080"
                );
    }

    @Test
    void shouldCompleteFullBikeRentalWorkflow() {

        long workflowStarted = System.nanoTime();
        log.info("Starting full bike reservation and rental workflow");

        /*
         * ============================================================
         * 1. REGISTER USER
         * ============================================================
         */

        log.info("Step 1/9: Register user");

        String username =
                "user-" + UUID.randomUUID();

        String password =
                "password123";

        given()
                .filter(HTTP_LOGGING)
                .baseUri(gatewayUrl)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "username": "%s",
                            "password": "%s"
                        }
                        """.formatted(
                        username,
                        password
                ))
                .when()
                .post("/api/v1/auth/register")
                .then()
                .statusCode(201)
                .body(
                        "accessToken",
                        notNullValue()
                );


        /*
         * ============================================================
         * 2. LOGIN
         * ============================================================
         */

        log.info("Step 2/9: Log in");

        String accessToken =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "username": "%s",
                                    "password": "%s"
                                }
                                """.formatted(
                                username,
                                password
                        ))
                        .when()
                        .post("/api/v1/auth/login")
                        .then()
                        .statusCode(200)
                        .body(
                                "accessToken",
                                notNullValue()
                        )
                        .extract()
                        .path("accessToken");


        /*
         * JWT subject is the auth-service user UUID.
         */
        String userId =
                extractSubjectFromJwt(accessToken);

        log.debug("Authenticated test user: userId={}", userId);
        assertThat(userId)
                .isNotBlank();


        /*
         * ============================================================
         * 3. CREATE STATION
         * ============================================================
         */

        log.info("Step 3/9: Create station");

        String stationId =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .auth()
                        .oauth2(accessToken)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "name": "Central Park Station",
                                    "address": "123 Park Ave",
                                    "capacity": 10
                                }
                                """)
                        .when()
                        .post("/api/v1/stations")
                        .then()
                        .statusCode(201)
                        .body(
                                "id",
                                notNullValue()
                        )
                        .extract()
                        .path("id");


        /*
         * ============================================================
         * 4. CREATE BIKE FOR RESERVATION
         * ============================================================
         */

        log.debug("Station created: stationId={}", stationId);
        log.info("Step 4/9: Create bike for reservation");

        String reservationBikeId =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .auth()
                        .oauth2(accessToken)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "serialNumber": "%s",
                                    "type": "ELECTRIC",
                                    "stationId": "%s"
                                }
                                """.formatted(
                                uniqueSerialNumber(),
                                stationId
                        ))
                        .when()
                        .post("/api/v1/bikes")
                        .then()
                        .statusCode(201)
                        .body(
                                "id",
                                notNullValue()
                        )
                        .extract()
                        .path("id");


        /*
         * ============================================================
         * 5. CREATE SECOND BIKE FOR RENTAL
         * ============================================================
         */

        log.debug("Reservation bike created: bikeId={}", reservationBikeId);
        log.info("Step 5/9: Create bike for rental");

        String rentalBikeId =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .auth()
                        .oauth2(accessToken)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "serialNumber": "%s",
                                    "type": "STANDARD",
                                    "stationId": "%s"
                                }
                                """.formatted(
                                uniqueSerialNumber(),
                                stationId
                        ))
                        .when()
                        .post("/api/v1/bikes")
                        .then()
                        .statusCode(201)
                        .body(
                                "id",
                                notNullValue()
                        )
                        .extract()
                        .path("id");


        /*
         * ============================================================
         * 6. CREATE RESERVATION
         * ============================================================
         */

        log.debug("Rental bike created: bikeId={}", rentalBikeId);
        log.info("Step 6/9: Create reservation");

        String reservationId =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .auth()
                        .oauth2(accessToken)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "userId": "%s",
                                    "bikeId": "%s",
                                    "durationHours": 2
                                }
                                """.formatted(
                                userId,
                                reservationBikeId
                        ))
                        .when()
                        .post("/api/v1/reservations")
                        .then()
                        .statusCode(201)
                        .body(
                                "id",
                                notNullValue()
                        )
                        .body(
                                "bikeId",
                                equalTo(reservationBikeId)
                        )
                        .body(
                                "status",
                                equalTo("ACTIVE")
                        )
                        .extract()
                        .path("id");


        log.debug("Reservation created: reservationId={}, bikeId={}", reservationId, reservationBikeId);
        assertThat(reservationId)
                .isNotBlank();


        /*
         * ============================================================
         * 7. START RENTAL
         * ============================================================
         */

        log.info("Step 7/9: Start rental");

        String rentalId =
                given()
                        .filter(HTTP_LOGGING)
                        .baseUri(gatewayUrl)
                        .auth()
                        .oauth2(accessToken)
                        .contentType(ContentType.JSON)
                        .body("""
                                {
                                    "userId": "%s",
                                    "bikeId": "%s",
                                    "stationId": "%s",
                                    "dailyRate": 100.00
                                }
                                """.formatted(
                                userId,
                                rentalBikeId,
                                stationId
                        ))
                        .when()
                        .post("/api/v1/rentals")
                        .then()
                        .statusCode(200)
                        .body(
                                "id",
                                notNullValue()
                        )
                        .body(
                                "bikeId",
                                equalTo(rentalBikeId)
                        )
                        .body(
                                "status",
                                equalTo("ACTIVE")
                        )
                        .extract()
                        .path("id");


        /*
         * ============================================================
         * 8. RETURN RENTAL
         * ============================================================
         */

        log.debug("Rental started: rentalId={}, bikeId={}", rentalId, rentalBikeId);
        log.info("Step 8/9: Return rental");

        given()
                .filter(HTTP_LOGGING)
                .baseUri(gatewayUrl)
                .auth()
                .oauth2(accessToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "stationId": "%s"
                        }
                        """.formatted(stationId))
                .when()
                .post(
                        "/api/v1/rentals/{rentalId}/return",
                        rentalId
                )
                .then()
                .statusCode(200)
                .body(
                        "id",
                        equalTo(rentalId)
                )
                .body(
                        "status",
                        equalTo("RETURNED")
                )
                .body(
                        "returnStationId",
                        equalTo(stationId)
                )
                .body(
                        "totalAmount",
                        notNullValue()
                );


        /*
         * ============================================================
         * 9. COMPLETE RENTAL
         * ============================================================
         */

        log.debug("Rental returned: rentalId={}, stationId={}", rentalId, stationId);
        log.info("Step 9/9: Complete rental");

        given()
                .filter(HTTP_LOGGING)
                .baseUri(gatewayUrl)
                .auth()
                .oauth2(accessToken)
                .when()
                .post(
                        "/api/v1/rentals/{rentalId}/complete",
                        rentalId
                )
                .then()
                .statusCode(200)
                .body(
                        "id",
                        equalTo(rentalId)
                )
                .body(
                        "status",
                        equalTo("COMPLETED")
                );
        log.debug("Rental completed: rentalId={}", rentalId);
        log.info("Full bike reservation and rental workflow passed in {} ms",
                (System.nanoTime() - workflowStarted) / 1_000_000);
    }

    private static String uniqueSerialNumber() {
        return "SN-" + UUID.randomUUID();
    }

    private static String extractSubjectFromJwt(
            String jwt
    ) {
        String[] parts =
                jwt.split("\\.");

        if (parts.length != 3) {
            throw new IllegalArgumentException(
                    "Invalid JWT"
            );
        }

        byte[] decoded =
                Base64.getUrlDecoder()
                        .decode(parts[1]);

        String payload =
                new String(
                        decoded,
                        StandardCharsets.UTF_8
                );

        return io.restassured.path.json.JsonPath
                .from(payload)
                .getString("sub");
    }
}
