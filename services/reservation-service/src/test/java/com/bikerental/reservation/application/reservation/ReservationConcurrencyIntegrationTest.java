package com.bikerental.reservation.application.reservation;

import com.bikerental.reservation.application.bike.BikeReservationDetails;
import com.bikerental.reservation.application.bike.BikeReservationGateway;
import com.bikerental.reservation.domain.reservation.Reservation;
import com.bikerental.reservation.domain.reservation.ReservationRepository;
import com.bikerental.reservation.domain.reservation.ReservationStatus;
import com.bikerental.reservation.infrastructure.persistence.reservation.SpringDataReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public class ReservationConcurrencyIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine")
                    .withDatabaseName("rentaldb")
                    .withUsername("reservation")
                    .withPassword("reservation");

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private SpringDataReservationRepository springDataReservationRepository;

    @MockitoBean
    private BikeReservationGateway bikeReservationGateway;

    @DynamicPropertySource
    static void configureProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );
    }

    @BeforeEach
    void setUp() {
        springDataReservationRepository.deleteAll();
        reset(bikeReservationGateway);
    }

    @Test
    void shouldAllowOnlyOneActiveReservationForSameUser() throws Exception {
        UUID userId = UUID.randomUUID();

        UUID firstBikeId = UUID.randomUUID();
        UUID secondBikeId = UUID.randomUUID();

        UUID stationId = UUID.randomUUID();

        when(bikeReservationGateway.reserveBike(eq(firstBikeId), any(UUID.class)))
                .thenReturn(new BikeReservationDetails(firstBikeId, stationId));
        when(bikeReservationGateway.reserveBike(eq(secondBikeId), any(UUID.class)))
                .thenReturn(new BikeReservationDetails(secondBikeId, stationId));
        when(bikeReservationGateway.reserveBike(any(), any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID bikeId = invocation.getArgument(0);
                    return new BikeReservationDetails(bikeId, stationId);
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Reservation> firstRequest = executor.submit(() -> {
            startLatch.await();

            return reservationService.createReservation(
                    userId,
                    firstBikeId,
                    Duration.ofHours(24)
            );
        });

        Future<Reservation> secondRequest =
                executor.submit(() -> {
                    startLatch.await();

                    return reservationService.createReservation(
                            userId,
                            secondBikeId,
                            Duration.ofHours(24)
                    );
                });

        startLatch.countDown();

        Result<Reservation> firstResult = getResult(firstRequest);
        Result<Reservation> secondResult = getResult(secondRequest);

        executor.shutdown();

        long successCount = Stream.of(firstResult, secondResult).filter(Result::isSuccess).count();

        long failureCount = Stream.of(firstResult, secondResult).filter(result -> !result.isSuccess()).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        List<Reservation> activeReservations =
                reservationRepository
                        .allReservations()
                        .stream()
                        .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                        .toList();
        assertThat(activeReservations).hasSize(1);
        assertThat(activeReservations.getFirst().getUserId()).isEqualTo(userId);
    }

    @Test
    void shouldAllowOnlyOneActiveReservationForSameBike() {
        UUID bikeId = UUID.randomUUID();

        UUID firstUserId = UUID.randomUUID();
        UUID secondUserId = UUID.randomUUID();

        UUID stationId = UUID.randomUUID();

        when(bikeReservationGateway.reserveBike(eq(bikeId), any(UUID.class)))
                .thenReturn(new BikeReservationDetails(bikeId, stationId));
        when(bikeReservationGateway.reserveBike(any(), any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID _bikeId = invocation.getArgument(0);
                    return new BikeReservationDetails(_bikeId, stationId);
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Reservation> firstRequest = executor.submit(() -> {
            startLatch.await();

            return reservationService.createReservation(
                    firstUserId,
                    bikeId,
                    Duration.ofHours(24)
            );
        });

        Future<Reservation> secondRequest =
                executor.submit(() -> {
                    startLatch.await();

                    return reservationService.createReservation(
                            secondUserId,
                            bikeId,
                            Duration.ofHours(24)
                    );
                });

        startLatch.countDown();

        Result<Reservation> firstResult = getResult(firstRequest);
        Result<Reservation> secondResult = getResult(secondRequest);

        executor.shutdown();

        long successCount = Stream.of(firstResult, secondResult).filter(Result::isSuccess).count();

        long failureCount = Stream.of(firstResult, secondResult).filter(result -> !result.isSuccess()).count();

        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);

        List<Reservation> activeReservations =
                reservationRepository
                        .allReservations()
                        .stream()
                        .filter(reservation -> reservation.getStatus() == ReservationStatus.ACTIVE)
                        .toList();
        assertThat(activeReservations).hasSize(1);
        assertThat(activeReservations.getFirst().getBikeId()).isEqualTo(bikeId);
    }

    @Test
    void shouldAllowReservationAfterPreviousReservationExpired() {

        UUID userId = UUID.randomUUID();

        UUID oldBikeId = UUID.randomUUID();
        UUID newBikeId = UUID.randomUUID();

        UUID stationId = UUID.randomUUID();

        Instant oldReservedAt = Instant.now().minus(Duration.ofDays(2));
        Instant oldExpiresAt = Instant.now().minus(Duration.ofDays(1));

        Reservation expiredReservation = Reservation.restore(
                UUID.randomUUID(),
                userId,
                oldBikeId,
                stationId,
                oldReservedAt,
                oldExpiresAt,
                ReservationStatus.EXPIRED,
                null,
                oldReservedAt,
                oldExpiresAt
        );

        reservationRepository.save(expiredReservation);

        when(bikeReservationGateway.reserveBike(eq(newBikeId), any(UUID.class)))
                .thenReturn(new BikeReservationDetails(newBikeId, stationId));

        Reservation newReservation = reservationService.createReservation(
                userId,
                newBikeId,
                Duration.ofHours(24)
        );

        assertThat(newReservation.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(reservationRepository.allReservations()).hasSize(2);
    }

    @Test
    void shouldReleaseBikeWhenDatabaseRejectsConcurrentReservation() throws Exception {
        UUID userId = UUID.randomUUID();

        UUID firstBikeId = UUID.randomUUID();
        UUID secondBikeId = UUID.randomUUID();

        UUID stationId = UUID.randomUUID();

        when(bikeReservationGateway.reserveBike(any(), any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID bikeId = invocation.getArgument(0);
                    return new BikeReservationDetails(bikeId, stationId);
                });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);

        Future<Reservation> firstRequest = executor.submit(() -> {
            startLatch.await();

            return reservationService.createReservation(
                    userId,
                    firstBikeId,
                    Duration.ofHours(24)
            );
        });

        Future<Reservation> secondRequest =
                executor.submit(() -> {
                    startLatch.await();

                    return reservationService.createReservation(
                            userId,
                            secondBikeId,
                            Duration.ofHours(24)
                    );
                });

        startLatch.countDown();

        Result<Reservation> firstResult = getResult(firstRequest);
        Result<Reservation> secondResult = getResult(secondRequest);

        executor.shutdown();

        verify(bikeReservationGateway, times(2)).reserveBike(any(), any(UUID.class));
        verify(bikeReservationGateway, atLeastOnce()).releaseBike(any(), any(UUID.class));

        long successCount = Stream.of(firstResult, secondResult).filter(Result::isSuccess).count();

        assertThat(successCount).isEqualTo(1);
    }

    private <T> Result<T> getResult(
            Future<T> future
    ) {
        try {
            return Result.success(future.get(10, TimeUnit.SECONDS));
        } catch (ExecutionException exception) {
            return Result.failure(exception.getCause());
        } catch (Exception exception) {
            return Result.failure(exception);
        }
    }

    private record Result<T>(
            T value,
            Throwable error
    ) {
        static <T> Result<T> success(T value) {
            return new Result<>(value, null);
        }

        static <T> Result<T> failure(Throwable error) {
            return new Result<>(null, error);
        }

        boolean isSuccess() {
            return error == null;
        }
    }
}
