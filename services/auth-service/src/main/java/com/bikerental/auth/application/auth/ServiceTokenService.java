package com.bikerental.auth.application.auth;

import com.bikerental.auth.api.auth.TokenResponse;
import com.bikerental.auth.infrastructure.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
public class ServiceTokenService {

    public static final long TTL_SECONDS = 300;

    private final JwtService jwtService;
    private final String reservationSecret;
    private final String rentalSecret;

    public ServiceTokenService(
            JwtService jwtService,
            @Value("${auth.service-clients.reservation-secret}")
            String reservationSecret,
            @Value("${auth.service-clients.rental-secret}")
            String rentalSecret
    ) {
        if (reservationSecret.isBlank() || rentalSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Service client secrets must not be blank"
            );
        }

        this.jwtService = jwtService;
        this.reservationSecret = reservationSecret;
        this.rentalSecret = rentalSecret;
    }

    public TokenResponse issue(String clientId, String clientSecret) {
        String expectedSecret;
        String scope;

        switch (clientId) {
            case "reservation-service" -> {
                expectedSecret = reservationSecret;
                scope = "bike:reservation";
            }
            case "rental-service" -> {
                expectedSecret = rentalSecret;
                scope = "bike:rental";
            }
            default -> throw invalidCredentials();
        }

        boolean matches = MessageDigest.isEqual(
                expectedSecret.getBytes(StandardCharsets.UTF_8),
                clientSecret.getBytes(StandardCharsets.UTF_8)
        );

        if (!matches) {
            throw invalidCredentials();
        }

        return new TokenResponse(
                jwtService.createServiceAccessToken(
                        clientId, scope, TTL_SECONDS
                ),
                "Bearer",
                TTL_SECONDS
        );
    }

    private ResponseStatusException invalidCredentials() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Invalid service credentials"
        );
    }

}
