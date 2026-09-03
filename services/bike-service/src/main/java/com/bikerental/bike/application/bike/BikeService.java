package com.bikerental.bike.application.bike;

import com.bikerental.bike.domain.bike.Bike;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BikeService {

    private final BikeRepository bikeRepository;

    public BikeService(BikeRepository bikeRepository) {
        this.bikeRepository = bikeRepository;
    }

    public Bike create(CreateBikeCommand command) {

        if (bikeRepository.existsBySerialNumber(command.serialNumber())) {
            throw new DuplicateBikeSerialNumberException(
                    command.serialNumber()
            );
        }

        Bike bike = Bike.create(
                command.serialNumber(),
                command.type(),
                command.stationId()
        );

        return bikeRepository.save(bike);
    }
}
