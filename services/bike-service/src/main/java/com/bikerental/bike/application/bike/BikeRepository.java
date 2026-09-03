package com.bikerental.bike.application.bike;

import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeFilter;
import com.bikerental.bike.infrastructure.persistence.bike.BikeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BikeRepository {

    Bike save(Bike bike);

    Optional<Bike> findById(UUID bikeId);

    List<Bike> findAll();

    Page<Bike> findAll(BikeFilter filter, Pageable pageable);

    boolean existsBySerialNumber(String serialNumber);
}
