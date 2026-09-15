package com.bikerental.rental.integration.rental;

import com.bikerental.rental.domain.rental.Rental;
import com.bikerental.rental.domain.rental.RentalStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.bikerental.rental.domain.rental.RentalRepository;
import com.bikerental.rental.integration.AbstractPostgresIntegrationTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Sql(scripts = "/sql/cleanup-rentals.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
class RentalRepositoryIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    RentalRepository adapter;

    @Test
    void shouldReturnEmptyForMissingRental() {
        assertThat(adapter.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void shouldEnforceOneActiveRentalPerUserInDatabase() {
        Rental first = adapter.save(newRental());
        Rental duplicate = Rental.start(first.getUserId(), UUID.randomUUID(), UUID.randomUUID(),
                first.getDailyRate(), first.getStartedAt());

        assertThatThrownBy(() -> adapter.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(adapter.findById(duplicate.getId())).isEmpty();
        assertThat(adapter.findById(first.getId())).isPresent();
    }

    @Test
    void shouldEnforceOneActiveRentalPerBikeInDatabase() {
        Rental first = adapter.save(newRental());
        Rental duplicate = Rental.start(UUID.randomUUID(), first.getBikeId(), UUID.randomUUID(),
                first.getDailyRate(), first.getStartedAt());

        assertThatThrownBy(() -> adapter.save(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(adapter.findById(duplicate.getId())).isEmpty();
        assertThat(adapter.findById(first.getId())).isPresent();
    }

    private Rental newRental() {
        return Rental.start(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("25.00"), Instant.parse("2026-09-15T00:00:00Z"));
    }

    @Test
    void savesFullLifecycleAcrossTransactions() {
        Rental fresh = newRental();
        assertThat(fresh.getVersion()).isNull();
        Rental started = adapter.save(fresh);
        assertThat(started.getVersion()).isZero();

        Rental loaded = adapter.findById(started.getId()).orElseThrow();
        loaded.returnBike(UUID.randomUUID(), loaded.getStartedAt().plusSeconds(3600), new BigDecimal("25.00"));
        Rental returned = adapter.save(loaded);
        assertThat(returned.getVersion()).isEqualTo(1L);
        assertThat(adapter.findById(returned.getId()).orElseThrow().getVersion()).isEqualTo(1L);

        returned.complete(returned.getStartedAt().plusSeconds(7200));
        Rental completed = adapter.save(returned);
        assertThat(completed.getVersion()).isEqualTo(2L);
        assertThat(adapter.findById(completed.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalStatus.COMPLETED);
    }

    @Test
    void rejectsStaleSnapshot() {
        Rental started = adapter.save(newRental());
        Rental first = adapter.findById(started.getId()).orElseThrow();
        Rental stale = adapter.findById(started.getId()).orElseThrow();
        UUID station = UUID.randomUUID();
        first.returnBike(station, first.getStartedAt().plusSeconds(3600), new BigDecimal("25.00"));
        adapter.save(first);
        stale.returnBike(UUID.randomUUID(), stale.getStartedAt().plusSeconds(3600), new BigDecimal("50.00"));

        assertThatThrownBy(() -> adapter.save(stale)).isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(adapter.findById(started.getId()).orElseThrow().getReturnStationId()).isEqualTo(station);
    }
}
