package com.bikerental.rental.infrastructure.config;

import com.bikerental.rental.infrastructure.security.ServiceTokenProvider;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

public class BikeClientSecurityConfiguration {

    @Bean
    RequestInterceptor bikeServiceAuthentication(
            ServiceTokenProvider tokenProvider
    ) {
        return template -> {
            template.removeHeader(HttpHeaders.AUTHORIZATION);
            template.header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + tokenProvider.accessToken()
            );
        };
    }
}
