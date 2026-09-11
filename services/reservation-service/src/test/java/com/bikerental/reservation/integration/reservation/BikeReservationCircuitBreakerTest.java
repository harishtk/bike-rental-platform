package com.bikerental.reservation.integration.reservation;

import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.infrastructure.client.bike.BikeFeignClient;
import com.bikerental.reservation.integration.AbstractPostgresIntegrationTest;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
class BikeReservationCircuitBreakerTest
        extends AbstractPostgresIntegrationTest {

    @MockitoBean
    private BikeFeignClient bikeFeignClient;

    @Autowired
    private BikeReservationGateway bikeReservationGateway;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreaker() {
        circuitBreakerRegistry
                .circuitBreaker("bike-service")
                .reset();
    }

    @Test
    void shouldOpenCircuitAfterRepeatedBikeServiceFailures() {
        UUID bikeId = UUID.randomUUID();

        when(bikeFeignClient.reserveBike(eq(bikeId), any(UUID.class)))
                .thenThrow(
                        new RuntimeException(
                                "Bike service unavailable"
                        )
                );

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() ->
                    bikeReservationGateway.reserveBike(bikeId, UUID.randomUUID())
            );
        }

        CircuitBreaker circuitBreaker =
                circuitBreakerRegistry
                        .circuitBreaker("bike-service");

        assertThat(circuitBreaker.getState())
                .isEqualTo(
                        CircuitBreaker.State.OPEN
                );
    }
}