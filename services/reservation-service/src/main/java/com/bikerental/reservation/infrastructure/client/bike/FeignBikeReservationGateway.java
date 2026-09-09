package com.bikerental.reservation.infrastructure.client.bike;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@RequiredArgsConstructor
@Component
public class FeignBikeReservationGateway
    implements BikeReservationGateway {

    private final BikeFeignClient bikeFeignClient;

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
