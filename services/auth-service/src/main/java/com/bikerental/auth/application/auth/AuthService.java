package com.bikerental.auth.application.auth;

import com.bikerental.auth.api.auth.LoginRequest;
import com.bikerental.auth.api.auth.RegisterRequest;
import com.bikerental.auth.api.auth.TokenResponse;
import com.bikerental.auth.domain.user.AuthUser;
import com.bikerental.auth.domain.user.AuthUserRepository;
import com.bikerental.auth.infrastructure.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final Clock clock;

    @Transactional
    public TokenResponse register(
            RegisterRequest request
    ) {
        String username =
                request.username().trim();

        if (userRepository.existsByUsername(
                username
        )) {
            throw new UsernameAlreadyExistsException(
                    username
            );
        }

        AuthUser user =
                AuthUser.create(
                        username,
                        passwordEncoder.encode(
                                request.password()
                        ),
                        Instant.now(clock)
                );

        AuthUser saved =
                userRepository.save(user);

        return token(saved);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(
            LoginRequest request
    ) {
        AuthUser user =
                userRepository
                        .findByUsername(
                                request.username()
                                        .trim()
                        )
                        .orElseThrow(
                                InvalidCredentialsException::new
                        );

        if (!passwordEncoder.matches(
                request.password(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException();
        }

        return token(user);
    }

    private TokenResponse token(
            AuthUser user
    ) {
        return new TokenResponse(
                jwtService.createAccessToken(user),
                "Bearer",
                jwtService.getAccessTokenTtl()
        );
    }
}