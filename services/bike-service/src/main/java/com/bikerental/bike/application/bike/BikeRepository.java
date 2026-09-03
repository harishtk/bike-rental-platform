package com.bikerental.bike.application.bike;

import com.bikerental.bike.domain.bike.Bike;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BikeRepository {

    Bike save(Bike bike);

    Optional<Bike> findById(UUID bikeId);

    List<Bike> findAll();

    boolean existsBySerialNumber(String serialNumber);
}
