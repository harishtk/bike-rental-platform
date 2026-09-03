package com.bikerental.bike.application.bike;

import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.infrastructure.persistence.bike.BikeEntity;
import com.bikerental.bike.infrastructure.persistence.bike.BikeEntitySpecifications;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    public Bike getById(UUID bikeId) {
        return bikeRepository.findById(bikeId)
                .orElseThrow(() -> new BikeNotFoundException(bikeId));
    }

    public List<Bike> getAll() {
        return bikeRepository.findAll();
    }

    public Page<Bike> getAll(UUID stationId, String status, Pageable pageable) {
        Specification<BikeEntity> spec = Specification.anyOf();
        if (stationId != null) {
            spec = spec.and(BikeEntitySpecifications.hasStationId(stationId));
        }
        if (status != null) {
            spec = spec.and(BikeEntitySpecifications.hasStatus(status));
        }
        return bikeRepository.findAll(spec, pageable);
    }

    @Transactional
    public Bike reserve(UUID bikeId) {
        Bike bike = getById(bikeId);
        bike.reserve();
        return bikeRepository.save(bike);
    }

    public Bike release(UUID bikeId) {
        Bike bike = getById(bikeId);
        bike.release();
        return bikeRepository.save(bike);
    }

    public Bike startMaintenance(UUID bikeId) {
        Bike bike = getById(bikeId);
        bike.startMaintenance();
        return bikeRepository.save(bike);
    }

    public Bike completeMaintenance(UUID bikeId) {
        Bike bike = getById(bikeId);
        bike.completeMaintenance();
        return bikeRepository.save(bike);
    }

    public Bike retire(UUID bikeId) {
        Bike bike = getById(bikeId);
        bike.retire();
        return bikeRepository.save(bike);
    }
}
