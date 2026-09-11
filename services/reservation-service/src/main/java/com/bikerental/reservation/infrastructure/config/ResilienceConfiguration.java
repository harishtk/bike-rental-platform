package com.bikerental.reservation.infrastructure.config;

import com.bikerental.reservation.application.bike.BikeServiceUnavailableException;
import io.github.resilience4j.common.retry.configuration.RetryConfigCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResilienceConfiguration {

    @Bean
    public RetryConfigCustomizer bikeServiceRetryCustomizer() {
        return RetryConfigCustomizer.of(
                "bike-service",
                builder -> builder.retryOnException(
                        exception ->
                                exception instanceof
                                        BikeServiceUnavailableException
                )
        );
    }
}