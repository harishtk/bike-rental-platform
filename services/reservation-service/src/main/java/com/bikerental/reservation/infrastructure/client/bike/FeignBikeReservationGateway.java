package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FeignBikeReservationGateway
    implements BikeReservationGateway {

    private final BikeFeignClient bikeFeignClient;

    public FeignBikeReservationGateway(BikeFeignClient bikeFeignClient) {
        this.bikeFeignClient = bikeFeignClient;
    }

    @Override
    public BikeReservationDetails reserveBike(UUID bikeId) {
        BikeFeignResponse response = bikeFeignClient.reserveBike(bikeId);
        return new BikeReservationDetails(
                response.id(),
                response.stationId()
        );
    }

    @Override
    public void releaseBike(UUID bikeId) {
        bikeFeignClient.releaseBike(bikeId);
    }
}
