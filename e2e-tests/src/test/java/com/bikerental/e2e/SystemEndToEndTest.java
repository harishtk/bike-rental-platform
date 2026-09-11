package com.bikerental.e2e;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.notNullValue;

public class SystemEndToEndTest {

    private static String bikeServiceUrl;
    private static String reservationServiceUrl;

    @BeforeAll
    static void setup() {
        // Fall back to localhost if env variables aren't provided
        String bikeHost = System.getenv().getOrDefault("BIKE_SERVICE_URL", "http://localhost:8000");
        String reservationHost = System.getenv().getOrDefault("RESERVATION_SERVICE_URL", "http://localhost:8001");

        bikeServiceUrl = bikeHost + "/api/v1";
        reservationServiceUrl = reservationHost + "/api/v1";
    }

    @Test
    void shouldCreateStationBikeAndReservationSuccessfully() {
        // 1. Create Station
        String stationId = given()
                .baseUri(bikeServiceUrl)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "name": "Central Park Station",
                            "address": "123 Park Ave",
                            "capacity": 10
                        }
                        """)
                .when().post("/stations")
                .then().statusCode(201)
                .extract().path("id");

        // 2. Create Bike
        String bikeId = given()
                .baseUri(bikeServiceUrl)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "serialNumber": "SN-998877",
                            "type": "ELECTRIC",
                            "stationId": "%s"
                        }
                        """.formatted(stationId))
                .when().post("/bikes")
                .then().statusCode(201)
                .extract().path("id");

        // 3. Create Reservation
        given()
                .baseUri(reservationServiceUrl)
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "userId": "%s",
                            "bikeId": "%s",
                            "durationHours": 2
                        }
                        """.formatted(UUID.randomUUID().toString(), bikeId))
                .when().post("/reservations")
                .then().statusCode(201)
                .body("id", notNullValue());
    }
}