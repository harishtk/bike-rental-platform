package com.bikerental.bike.integration.station;

import com.bikerental.bike.domain.station.Station;
import com.bikerental.bike.domain.station.StationRepository;
import com.bikerental.bike.domain.station.StationStatus;
import com.bikerental.bike.integration.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@Sql(
        scripts = "/sql/cleanup-stations.sql",
        executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD
)
public class StationRepositoryIntegrationTest extends
        AbstractPostgresIntegrationTest {

    @Autowired
    private StationRepository stationRepository;

    @BeforeEach
    void setUp() {
        // Repository cleanup can be added here later.
    }

    @Test
    void shouldSaveAndRetrieveStation() {
        Station station = Station.create(
                "Central Station",
                "12 MG Road, Bengaluru",
                25
        );

        Station saved = stationRepository.save(station);

        assertThat(saved.getId()).isEqualTo(station.getId());
        assertThat(saved.getName()).isEqualTo("Central Station");
        assertThat(saved.getAddress()).isEqualTo("12 MG Road, Bengaluru");
        assertThat(saved.getCapacity()).isEqualTo(25);
        assertThat(saved.getStatus()).isEqualTo(StationStatus.ACTIVE);

        Station retrieved = stationRepository
                .findById(station.getId())
                .orElseThrow();

        assertThat(retrieved.getId()).isEqualTo(station.getId());
        assertThat(retrieved.getName()).isEqualTo("Central Station");
        assertThat(retrieved.getCapacity()).isEqualTo(25);
    }

    @Test
    void shouldReturnEmptyStationWhenStationDoesNotExist() {
        UUID stationId = UUID.randomUUID();

        assertThat(stationRepository.findById(stationId)).isEmpty();
    }

    @Test
    void shouldFindAllStations() {
        stationRepository.save(
                Station.create(
                        "Central Station",
                        "12 MG Road, Bengaluru",
                        25
                )
        );
        stationRepository.save(
                Station.create(
                        "North Station",
                        "45 Residency Road, Bengaluru",
                        15
                )
        );

        List<Station> stations = stationRepository.findAll();

        assertThat(stations)
                .hasSize(2)
                .extracting(Station::getName)
                .containsExactlyInAnyOrder(
                        "Central Station",
                        "North Station"
                );
    }
}
