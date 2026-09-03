package com.bikerental.bike.integration.bike;

import com.bikerental.bike.application.bike.BikeRepository;
import com.bikerental.bike.domain.bike.Bike;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
public class BikeOptimisticLockingIntegrationTest {

    @Autowired
    private BikeRepository bikeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldThrowOptimisticLockExceptionForStaleBikeUpdate() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);

        Bike savedBike = transaction.execute(status ->
                bikeRepository.save(createAvailableBike()));
        Assertions.assertNotNull(savedBike);

        UUID bikeId = savedBike.getId();

        Bike firstCopy = transaction.execute(status ->
                bikeRepository.findById(bikeId).orElseThrow());

        Bike secondCopy = transaction.execute(status ->
                bikeRepository.findById(bikeId).orElseThrow());

        transaction.executeWithoutResult(status -> {
            Assertions.assertNotNull(firstCopy);
            firstCopy.reserve();
            bikeRepository.save(firstCopy);
        });

        assertThatThrownBy(() -> {
            transaction.executeWithoutResult(status -> {
                Assertions.assertNotNull(secondCopy);
                secondCopy.reserve();
                bikeRepository.save(secondCopy);
            });
        }).isInstanceOf(OptimisticLockingFailureException.class);
    }

    private Bike createAvailableBike() {
        return Bike.create("BIKE-" + UUID.randomUUID(), "Off-Road", UUID.randomUUID());
    }
}
