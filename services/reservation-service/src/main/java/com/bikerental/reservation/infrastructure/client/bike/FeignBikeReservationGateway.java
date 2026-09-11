package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.application.bike.*;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class FeignBikeReservationGateway
    implements BikeReservationGateway {

    private final BikeFeignClient bikeFeignClient;

    @Retry(name = "bike-service")
    @CircuitBreaker(name = "bike-service")
    @Override
    public BikeReservationDetails reserveBike(UUID bikeId, UUID operationId) {
        try {
            BikeFeignResponse response = bikeFeignClient.reserveBike(bikeId, operationId);
            return new BikeReservationDetails(
                    response.id(),
                    response.stationId()
            );
        } catch (FeignException.NotFound exception) {
            throw new BikeNotFoundException(bikeId);
        } catch (FeignException.Conflict exception) {
            throw new BikeNotReservableException(bikeId);
        } catch (FeignException exception) {
            throw new BikeServiceUnavailableException(
                    "Bike service request failed for bike: " + bikeId,
                    exception
            );
        }
    }

    @Retry(name = "bike-service")
    @CircuitBreaker(name = "bike-service")
    @Override
    public void releaseBike(UUID bikeId, UUID operationId) {
        try {
            bikeFeignClient.releaseBike(bikeId, operationId);
        } catch (FeignException.NotFound exception) {
            throw new BikeNotFoundException(bikeId);
        } catch (FeignException exception) {
            throw new BikeServiceUnavailableException(
                    "Failed to release bike: " + bikeId,
                    exception
            );
        }
    }
}
