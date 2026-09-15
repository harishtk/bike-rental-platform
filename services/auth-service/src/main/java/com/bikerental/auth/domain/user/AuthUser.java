package com.bikerental.auth.domain.user;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AuthUser {

    private final UUID id;
    private final String username;
    private final String password;
    private final UserRole role;
    private final Instant createdAt;
    private Instant updatedAt;

    public static AuthUser create(
            String username,
            String encodedPassword,
            Instant now
    ) {
        return new AuthUser(
                UUID.randomUUID(),
                username,
                encodedPassword,
                UserRole.USER,
                now,
                now
        );
    }

    public static AuthUser restore(
            UUID id,
            String username,
            String password,
            UserRole role,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AuthUser(
                id,
                username,
                password,
                role,
                createdAt,
                updatedAt
        );
    }

}