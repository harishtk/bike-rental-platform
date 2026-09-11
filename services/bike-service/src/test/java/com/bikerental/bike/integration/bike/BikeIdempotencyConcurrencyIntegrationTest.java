package com.bikerental.bike.integration.bike;

import com.bikerental.bike.application.bike.BikeReservationCommandService;
import com.bikerental.bike.application.bike.BikeReservationResult;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeRepository;
import com.bikerental.bike.infrastructure.persistence.idempotency.SpringDataBikeOperationRepository;
import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class BikeIdempotencyConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private BikeReservationCommandService reservationCommandService;

    @Autowired
    private SpringDataBikeOperationRepository bikeOperationRepository;

    @Autowired
    private BikeRepository bikeRepository;

    @BeforeEach
    void setUp() {
        bikeOperationRepository.deleteAll();
    }

    @Test
    void shouldProcessConcurrentReserveWithSameOperationIdOnlyOnce()
            throws Exception {

        UUID stationId = UUID.randomUUID();

        Bike bike =
                Bike.create(
                        "Bike-" + UUID.randomUUID(),
                        "Off-Road",
                        stationId
                );

        Bike savedBike =
                bikeRepository.save(bike);

        UUID bikeId =
                savedBike.getId();

        UUID operationId =
                UUID.randomUUID();

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        Callable<BikeReservationResult> task =
                () -> {
                    startLatch.await();

                    return reservationCommandService.reserveBike(
                            bikeId,
                            operationId
                    );
                };

        Future<BikeReservationResult> first =
                executor.submit(task);

        Future<BikeReservationResult> second =
                executor.submit(task);

        startLatch.countDown();

        BikeReservationResult firstResult =
                first.get(10, TimeUnit.SECONDS);

        BikeReservationResult secondResult =
                second.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertThat(firstResult.id())
                .isEqualTo(bikeId);

        assertThat(secondResult.id())
                .isEqualTo(bikeId);

        assertThat(firstResult.stationId())
                .isEqualTo(stationId);

        assertThat(secondResult.stationId())
                .isEqualTo(stationId);

        assertThat(
                bikeOperationRepository.count()
        ).isEqualTo(1);

        assertThat(
                bikeOperationRepository.findById(operationId)
        ).isPresent();
    }
}