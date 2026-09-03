package com.bikerental.bike.infrastructure.persistence.bike;

import com.bikerental.bike.application.bike.BikeRepository;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeFilter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BikeRepositoryAdapter implements BikeRepository {

    private final SpringDataBikeRepository repository;
    private final BikeMapper mapper;

    public BikeRepositoryAdapter(SpringDataBikeRepository repository, BikeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Bike save(Bike bike) {
        BikeEntity entity = mapper.toEntity(bike);

        BikeEntity savedEntity = repository.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Bike> findById(UUID bikeId) {
        return repository.findById(bikeId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Bike> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Page<Bike> findAll(BikeFilter filter, Pageable pageable) {
        return repository.findAll(buildSpecFromFilter(filter), pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsBySerialNumber(String serialNumber) {
        return repository.existsBySerialNumber(serialNumber);
    }

    private Specification<BikeEntity> buildSpecFromFilter(BikeFilter filter) {
        Specification<BikeEntity> spec = Specification.unrestricted();
        if (filter.stationId() != null) {
            spec = spec.and(BikeEntitySpecifications.hasStationId(filter.stationId()));
        }
        if (filter.bikeStatus() != null) {
            spec = spec.and(BikeEntitySpecifications.hasStatus(filter.bikeStatus()));
        }

        return spec;
    }
}
