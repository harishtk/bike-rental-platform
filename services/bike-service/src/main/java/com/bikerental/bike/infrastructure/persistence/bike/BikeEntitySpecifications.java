package com.bikerental.bike.infrastructure.persistence.bike;

import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class BikeEntitySpecifications {

    public static Specification<BikeEntity> hasStationId(UUID stationId) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("stationId"), stationId);
    }

    public static Specification<BikeEntity> hasStatus(String status) {
        return ((root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("status"), status));
    }
}
