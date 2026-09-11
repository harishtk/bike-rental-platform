package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import feign.FeignException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@SpringBootTest(
        properties = {
                "outbox.publisher.enabled=false",
                "resilience4j.retry.instances.bikeService.maxAttempts=3",
                "resilience4j.retry.instances.bikeService.waitDuration=1ms",
                "resilience4j.circuitbreaker.instances.bikeService.minimumNumberOfCalls=100"
        }
)
@ActiveProfiles("test")
class BikeReservationRetryTest extends AbstractPostgresIntegrationTest {

    @MockitoBean
    private BikeFeignClient bikeFeignClient;

    @Autowired
    private BikeReservationGateway bikeReservationGateway;

    @Test
    void shouldRetryUsingSameIdempotencyKey() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(
                bikeFeignClient.reserveBike(
                        bikeId,
                        operationId
                )
        )
                .thenThrow(serverError(bikeId))
                .thenThrow(serverError(bikeId))
                .thenReturn(
                        new BikeFeignResponse(
                                bikeId,
                                stationId
                        )
                );

        BikeReservationDetails result =
                bikeReservationGateway.reserveBike(
                        bikeId,
                        operationId
                );

        assertThat(result.bikeId())
                .isEqualTo(bikeId);

        assertThat(result.stationId())
                .isEqualTo(stationId);

        verify(
                bikeFeignClient,
                times(3)
        ).reserveBike(
                bikeId,
                operationId
        );
    }

    private FeignException serverError(UUID bikeId) {
        Request request =
                Request.create(
                        Request.HttpMethod.POST,
                        "/api/v1/bikes/"
                                + bikeId
                                + "/reserve",
                        Map.of(),
                        null,
                        StandardCharsets.UTF_8
                );

        Response response =
                Response.builder()
                        .status(500)
                        .reason("Internal Server Error")
                        .request(request)
                        .build();

        return FeignException.errorStatus(
                "reserveBike",
                response
        );
    }

    @Test
    void shouldNotRetryConflictResponse() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        Request request =
                Request.create(
                        Request.HttpMethod.POST,
                        "/api/v1/bikes/"
                                + bikeId
                                + "/reserve",
                        Map.of(),
                        null,
                        StandardCharsets.UTF_8
                );

        Response response =
                Response.builder()
                        .status(409)
                        .reason("Conflict")
                        .request(request)
                        .build();

        when(
                bikeFeignClient.reserveBike(
                        bikeId,
                        operationId
                )
        ).thenThrow(
                FeignException.errorStatus(
                        "reserveBike",
                        response
                )
        );

        assertThatThrownBy(() ->
                bikeReservationGateway.reserveBike(
                        bikeId,
                        operationId
                )
        ).isInstanceOf(
                com.bikerental.reservation.application.bike
                        .BikeNotReservableException.class
        );

        verify(
                bikeFeignClient,
                times(1)
        ).reserveBike(
                bikeId,
                operationId
        );
    }
}