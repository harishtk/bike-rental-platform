package com.bikerental.auth.api.auth;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}