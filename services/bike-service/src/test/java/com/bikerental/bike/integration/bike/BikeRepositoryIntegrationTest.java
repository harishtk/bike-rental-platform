package com.bikerental.bike.integration.bike;

import com.bikerental.bike.application.bike.BikeRepository;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeStatus;
import com.bikerental.bike.domain.bike.InvalidBikeStateException;
import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ActiveProfiles("test")
@Sql(
        scripts = "/sql/cleanup-bikes.sql",
        executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD
)
public class BikeRepositoryIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private BikeRepository bikeRepository;

    @BeforeEach
    void setUp() {
        // Repository cleanup can be added here later.
    }

    @Test
    void shouldSaveAndRetrieveBike() {
        UUID stationId = UUID.randomUUID();
        Bike bike = Bike.create(
                "1829319291923",
                "Off-Road",
                stationId
        );

        Bike saved = bikeRepository.save(bike);

        assertThat(saved.getId()).isEqualTo(bike.getId());
        assertThat(saved.getSerialNumber()).isEqualTo("1829319291923");
        assertThat(saved.getType()).isEqualTo("Off-Road");
        assertThat(saved.getStatus()).isEqualTo(BikeStatus.AVAILABLE);
        assertThat(saved.getVersion()).isEqualTo(0);

        Bike retrieved = bikeRepository
                .findById(bike.getId())
                .orElseThrow();

        assertThat(retrieved.getId()).isEqualTo(bike.getId());
        assertThat(retrieved.getSerialNumber()).isEqualTo("1829319291923");
        assertThat(retrieved.getVersion()).isEqualTo(0);
    }

    @Test
    void shouldReturnEmptyBikeWhenBikeDoesNotExist() {
        UUID bikeId = UUID.randomUUID();

        assertThat(bikeRepository.findById(bikeId)).isEmpty();
    }

    @Test
    void shouldFindAllBikes() {
        UUID stationId = UUID.randomUUID();
        bikeRepository.save(
                Bike.create(
                        "01",
                        "type-1",
                        stationId
                )
        );
        bikeRepository.save(
                Bike.create(
                        "02",
                        "type-2",
                        stationId
                )
        );

        List<Bike> bikes = bikeRepository.findAll();

        assertThat(bikes)
                .hasSize(2)
                .extracting(Bike::getSerialNumber)
                .containsExactlyInAnyOrder(
                        "01",
                        "02"
                );
    }

    @Test
    void shouldReserveTheBikeWhenAvailable() {
        UUID stationId = UUID.randomUUID();
        Bike bike = Bike.create(
                "01",
                "Off-Road",
                stationId
        );
        bike.reserve();

        Bike saved = bikeRepository.save(bike);

        assertThat(bikeRepository.existsBySerialNumber("01")).isTrue();
        assertThat(saved.getStatus()).isEqualTo(BikeStatus.RESERVED);
    }

    @Test
    void shouldReleaseTheBikeWhenReserved() {
        UUID stationId = UUID.randomUUID();
        Bike bike = Bike.create(
                "01",
                "Off-Road",
                stationId
        );
        bike.reserve();

        Bike reserved = bikeRepository.save(bike);
        reserved.release();

        Bike released = bikeRepository.save(reserved);

        assertThat(bikeRepository.existsBySerialNumber("01")).isTrue();
        assertThat(released.getStatus()).isEqualTo(BikeStatus.AVAILABLE);
    }

    @Test
    void shouldThrowWhenBikeIsRented() {
        UUID stationId = UUID.randomUUID();
        Bike bike = Bike.create(
                "01",
                "Off-Road",
                stationId
        );
        bike.rent();

        Bike rented = bikeRepository.save(bike);

        assertThatThrownBy(rented::reserve)
                .isInstanceOf(InvalidBikeStateException.class)
                .hasMessage("Expected bike status %s but was %s".formatted("AVAILABLE", "RENTED"));
    }
}
