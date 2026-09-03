package com.bikerental.bike.integration.bike;

import com.bikerental.bike.application.bike.BikeRepository;
import com.bikerental.bike.application.bike.BikeService;
import com.bikerental.bike.domain.bike.Bike;
import com.bikerental.bike.domain.bike.BikeStatus;
import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class BikeConcurrencyIntegrationTest
        extends AbstractPostgresIntegrationTest {

    @Autowired
    private BikeService bikeService;

    @Autowired
    private BikeRepository bikeRepository;

    @Test
    void shouldAllowOnlyOneSuccessfulConcurrentReservation()
        throws InterruptedException {

        // Given
        Bike bike = createAvailableBike();
        Bike savedBike = bikeRepository.save(bike);

        UUID bikeId = bike.getId();

        ExecutorService executorService =
                Executors.newFixedThreadPool(2);

        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(2);

        AtomicInteger successfulReservations = new AtomicInteger(0);

        List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());

        try {
            Runnable reserveBikeTask = () -> {
                try {
                    // Tell the test thread that this worker is ready.
                    readyLatch.countDown();

                    // Wait until both workers are ready.
                    boolean started = startLatch.await(5, TimeUnit.SECONDS);

                    if (!started) {
                        throw new IllegalStateException("Time out waiting for concurrent test to start");
                    }

                    // Attempt to reserve the same bike
                    bikeService.reserve(bikeId);

                    successfulReservations.incrementAndGet();
                } catch (Throwable throwable) {
                    failures.add(throwable);
                } finally {
                    completionLatch.countDown();
                }
            };

            // Submit two concurrent reservation attempts
            executorService.submit(reserveBikeTask);
            executorService.submit(reserveBikeTask);

            // Wait until both worker threads are ready.
            boolean workersReady = readyLatch.await(5, TimeUnit.SECONDS);

            assertThat(workersReady)
                    .as("Both worker threads should be ready")
                    .isTrue();

            // Release both workers at approx. the same time.
            startLatch.countDown();

            // Wait until both workers operations to finish.
            boolean completed = completionLatch.await(10, TimeUnit.SECONDS);

            assertThat(completed)
                    .as("Both reservation attempts should complete")
                    .isTrue();
        } finally {
            executorService.shutdown();
            boolean terminated = executorService.awaitTermination(5, TimeUnit.SECONDS);

            if (!terminated) {
                executorService.shutdownNow();
            }
        }

        // Then

        // Exactly one reservation must succeed.
        assertThat(successfulReservations.get()).isEqualTo(1);

        // Exactly one attempt must fail
        assertThat(failures).hasSize(1);

        // Verify the final persisted state.
        Bike updatedBike = bikeService.getById(bikeId);

        assertThat(updatedBike.getStatus()).isEqualTo(BikeStatus.RESERVED);

    }

    private Bike createAvailableBike() {
        return Bike.create("BIKE-" + UUID.randomUUID(), "Off-Road", UUID.randomUUID());
    }
}
