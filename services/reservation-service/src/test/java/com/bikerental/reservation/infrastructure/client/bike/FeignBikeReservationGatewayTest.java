package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.application.bike.BikeNotFoundException;
import com.bikerental.reservation.application.bike.BikeNotReservableException;
import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeServiceUnavailableException;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeignBikeReservationGatewayTest {

    @Mock
    private BikeFeignClient bikeFeignClient;

    private FeignBikeReservationGateway gateway;

    @BeforeEach
    void setUp() {
        gateway =
                new FeignBikeReservationGateway(
                        bikeFeignClient
                );
    }

    @Test
    void shouldReserveBike() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeFeignClient.reserveBike(bikeId, operationId))
                .thenReturn(
                        new BikeFeignResponse(
                                bikeId,
                                stationId
                        )
                );

        BikeReservationDetails result =
                gateway.reserveBike(bikeId, operationId);

        assertThat(result.bikeId())
                .isEqualTo(bikeId);

        assertThat(result.stationId())
                .isEqualTo(stationId);
    }

    @Test
    void shouldThrowBikeNotFoundWhenBikeDoesNotExist() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeFeignClient.reserveBike(bikeId, operationId))
                .thenThrow(
                        FeignException.errorStatus(
                                "reserveBike",
                                feign.Response.builder()
                                        .status(404)
                                        .reason("Not Found")
                                        .request(
                                                feign.Request.create(
                                                        feign.Request.HttpMethod.POST,
                                                        "/api/v1/bikes/" + bikeId + "/reserve",
                                                        Map.of(),
                                                        null,
                                                        StandardCharsets.UTF_8
                                                )
                                        )
                                        .build()
                        )
                );

        assertThatThrownBy(() ->
                gateway.reserveBike(bikeId, operationId)
        ).isInstanceOf(BikeNotFoundException.class);
    }

    @Test
    void shouldThrowBikeNotReservableOnConflict() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeFeignClient.reserveBike(bikeId, operationId))
                .thenThrow(
                        FeignException.errorStatus(
                                "reserveBike",
                                feign.Response.builder()
                                        .status(409)
                                        .reason("Conflict")
                                        .request(
                                                feign.Request.create(
                                                        feign.Request.HttpMethod.POST,
                                                        "/api/v1/bikes/" + bikeId + "/reserve",
                                                        Map.of(),
                                                        null,
                                                        StandardCharsets.UTF_8
                                                )
                                        )
                                        .build()
                        )
                );

        assertThatThrownBy(() ->
                gateway.reserveBike(bikeId, operationId)
        ).isInstanceOf(BikeNotReservableException.class);
    }

    @Test
    void shouldTranslateBikeServiceFailure() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(bikeFeignClient.reserveBike(bikeId, operationId))
                .thenThrow(
                        FeignException.errorStatus(
                                "reserveBike",
                                feign.Response.builder()
                                        .status(500)
                                        .reason("Internal Server Error")
                                        .request(
                                                feign.Request.create(
                                                        feign.Request.HttpMethod.POST,
                                                        "/api/v1/bikes/" + bikeId + "/reserve",
                                                        Map.of(),
                                                        null,
                                                        StandardCharsets.UTF_8
                                                )
                                        )
                                        .build()
                        )
                );

        assertThatThrownBy(() ->
                gateway.reserveBike(bikeId, operationId)
        )
                .isInstanceOf(
                        BikeServiceUnavailableException.class
                );
    }

    @Test
    void shouldReleaseBike() {
        UUID bikeId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        gateway.releaseBike(bikeId, operationId);

        verify(bikeFeignClient)
                .releaseBike(bikeId, operationId);
    }

    @Test
    void shouldReserveBikeUsingSameOperationId() {
        UUID bikeId = UUID.randomUUID();
        UUID stationId = UUID.randomUUID();
        UUID operationId = UUID.randomUUID();

        when(
                bikeFeignClient.reserveBike(
                        bikeId,
                        operationId
                )
        ).thenReturn(
                new BikeFeignResponse(
                        bikeId,
                        stationId
                )
        );

        BikeReservationDetails result =
                gateway.reserveBike(
                        bikeId,
                        operationId
                );

        assertThat(result.bikeId())
                .isEqualTo(bikeId);

        assertThat(result.stationId())
                .isEqualTo(stationId);

        verify(bikeFeignClient)
                .reserveBike(
                        bikeId,
                        operationId
                );
    }
}