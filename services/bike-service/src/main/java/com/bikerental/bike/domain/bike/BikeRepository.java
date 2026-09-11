package com.bikerental.bike.domain.bike;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BikeRepository {

    Bike save(Bike bike);

    Optional<Bike> findById(UUID bikeId);

    Optional<Bike> findByIdForUpdate(UUID bikeId);

    List<Bike> findAll();

    Page<Bike> findAll(BikeFilter filter, Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);
}
