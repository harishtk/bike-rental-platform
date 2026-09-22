package com.bikerental.reservation.api.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

public final class CustomerIdentity {

    private CustomerIdentity() {}

    public static UUID userId(Jwt jwt) {
        String subject = jwt == null ? null : jwt.getSubject();

        if (subject == null) {
            throw invalidIdentity();
        }

        try {
            UUID userId = UUID.fromString(subject);

            if (!userId.toString().equalsIgnoreCase(subject)) {
                throw invalidIdentity();
            }

            return userId;
        } catch (IllegalArgumentException e) {
            throw invalidIdentity();
        }
    }

    private static ResponseStatusException invalidIdentity() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "A valid customer identity is required."
        );
    }
}
