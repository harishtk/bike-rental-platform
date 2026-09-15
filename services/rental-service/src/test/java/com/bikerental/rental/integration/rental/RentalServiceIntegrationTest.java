package com.bikerental.rental.integration.rental;

import com.bikerental.rental.application.rental.RentalNotFoundException;
import com.bikerental.rental.application.rental.RentalService;
import com.bikerental.rental.domain.rental.Rental;
import com.bikerental.rental.domain.rental.RentalRepository;
import com.bikerental.rental.domain.rental.RentalStatus;
import com.bikerental.rental.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Sql(scripts = "/sql/cleanup-rentals.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RentalServiceIntegrationTest extends AbstractPostgresIntegrationTest {

    private static final Instant START = Instant.parse("2026-09-15T00:00:00Z");
    private static final BigDecimal DAILY_RATE = new BigDecimal("25.00");

    @Autowired
    private RentalService rentalService;

    @Autowired
    private RentalRepository rentalRepository;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void setTime() {
        when(clock.instant()).thenReturn(START);
    }

    @ParameterizedTest
    @CsvSource({"0, 25.00", "3600, 25.00", "86400, 25.00", "86401, 50.00", "172800, 50.00"})
    void shouldChargeAtLeastOneDayAndRoundUpPartialDays(long seconds, String expectedTotal) {
        Rental started = startRental();
        when(clock.instant()).thenReturn(START.plusSeconds(seconds));

        Rental returned = rentalService.returnRental(started.getId(), UUID.randomUUID());

        assertThat(returned.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        var stored = rentalRepository.findById(started.getId()).orElseThrow();
        assertThat(stored.getTotalAmount()).isEqualByComparingTo(expectedTotal);
        assertThat(stored.getReturnedAt()).isEqualTo(START.plusSeconds(seconds));
    }

    @Test
    void shouldRejectSecondActiveRentalForSameUser() {
        Rental first = startRental();
        UUID otherBike = UUID.randomUUID();

        assertThatThrownBy(() -> rentalService.startRental(first.getUserId(), otherBike,
                UUID.randomUUID(), DAILY_RATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("User already has an active rental");

        verify(bikeRentalGateway, never()).startRental(eq(otherBike), any(UUID.class));
        assertThat(rentalRepository.existsByBikeIdAndStatus(otherBike, RentalStatus.ACTIVE)).isFalse();
    }

    @Test
    void shouldRejectSecondActiveRentalForSameBike() {
        Rental first = startRental();
        UUID otherUser = UUID.randomUUID();

        assertThatThrownBy(() -> rentalService.startRental(otherUser, first.getBikeId(),
                UUID.randomUUID(), DAILY_RATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Bike already has an active rental");

        verify(bikeRentalGateway, times(1)).startRental(eq(first.getBikeId()), any(UUID.class));
        assertThat(rentalRepository.existsByUserIdAndStatus(otherUser, RentalStatus.ACTIVE)).isFalse();
    }

    @Test
    void shouldAllowNewRentalAfterPreviousRentalIsCompleted() {
        Rental first = startRental();
        rentalService.returnRental(first.getId(), UUID.randomUUID());
        rentalService.completeRental(first.getId());

        Rental next = rentalService.startRental(first.getUserId(), first.getBikeId(),
                first.getStartStationId(), DAILY_RATE);

        assertThat(next.getId()).isNotEqualTo(first.getId());
        assertThat(next.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(rentalRepository.findById(first.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalStatus.COMPLETED);
    }

    @Test
    void shouldRejectCompletionBeforeReturn() {
        Rental started = startRental();

        assertThatThrownBy(() -> rentalService.completeRental(started.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Rental must be returned before completion");

        assertThat(rentalRepository.findById(started.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalStatus.ACTIVE);
    }

    @Test
    void shouldRejectRepeatedReturnWithoutChangingStoredRental() {
        Rental started = startRental();
        UUID station = UUID.randomUUID();
        rentalService.returnRental(started.getId(), station);

        assertThatThrownBy(() -> rentalService.returnRental(started.getId(), UUID.randomUUID()))
                .isInstanceOf(IllegalStateException.class);

        Rental stored = rentalRepository.findById(started.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(RentalStatus.RETURNED);
        assertThat(stored.getReturnStationId()).isEqualTo(station);
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldRejectReturnOfMissingRental() {
        assertThatThrownBy(() -> rentalService.returnRental(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NoSuchElementException.class);
        verifyNoInteractions(bikeRentalGateway);
    }

    @Test
    void shouldRejectCompletionOfMissingRental() {
        UUID missingId = UUID.randomUUID();
        assertThatThrownBy(() -> rentalService.completeRental(missingId))
                .isInstanceOf(RentalNotFoundException.class)
                .hasMessageContaining(missingId.toString());
        verifyNoInteractions(bikeRentalGateway);
    }

    @Test
    void shouldNotCreateRentalWhenBikeServiceFails() {
        UUID user = UUID.randomUUID();
        UUID bike = UUID.randomUUID();
        doThrow(new IllegalStateException("Bike service unavailable"))
                .when(bikeRentalGateway).startRental(eq(bike), any(UUID.class));

        assertThatThrownBy(() -> rentalService.startRental(user, bike, UUID.randomUUID(), DAILY_RATE))
                .isInstanceOf(IllegalStateException.class).hasMessage("Bike service unavailable");

        assertThat(rentalRepository.existsByUserIdAndStatus(user, RentalStatus.ACTIVE)).isFalse();
        assertThat(rentalRepository.existsByBikeIdAndStatus(bike, RentalStatus.ACTIVE)).isFalse();
    }

    @Test
    void shouldKeepRentalActiveWhenBikeReturnFails() {
        Rental started = startRental();
        UUID station = UUID.randomUUID();
        doThrow(new IllegalStateException("Bike service unavailable"))
                .when(bikeRentalGateway).returnBike(eq(started.getBikeId()), eq(station), any(UUID.class));

        assertThatThrownBy(() -> rentalService.returnRental(started.getId(), station))
                .isInstanceOf(IllegalStateException.class).hasMessage("Bike service unavailable");

        Rental stored = rentalRepository.findById(started.getId()).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(stored.getReturnedAt()).isNull();
        assertThat(stored.getReturnStationId()).isNull();
        assertThat(stored.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(stored.getVersion()).isZero();
    }

    private Rental startRental() {
        return rentalService.startRental(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), DAILY_RATE);
    }
}
