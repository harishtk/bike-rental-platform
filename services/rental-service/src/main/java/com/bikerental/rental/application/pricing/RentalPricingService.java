package com.bikerental.rental.application.pricing;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

@Service
public class RentalPricingService {

    public BigDecimal calculate(
            Instant startedAt,
            Instant returnedAt,
            BigDecimal dailyRate
    ) {
        long seconds =
                Duration.between(
                        startedAt,
                        returnedAt
                ).getSeconds();

        long days =
                Math.max(
                        1,
                        (long) Math.ceil(
                                seconds / 86400.0
                        )
                );

        return dailyRate.multiply(
                BigDecimal.valueOf(days)
        );
    }
}