package com.bikerental.bike.infrastructure.persistence.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.List;

@Configuration
public class SecurityConfiguration {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    @Order(1)
    SecurityFilterChain serviceCommandFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityMatcher(
                        "/api/v1/bikes/*/reserve",
                        "/api/v1/bikes/*/release",
                        "/api/v1/bikes/*/rent",
                        "/api/v1/bikes/*/return"
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(serviceJwtDecoder())))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bikes/*/reserve",
                                "/api/v1/bikes/*/release"
                        ).hasAuthority("SCOPE_bike:reservation")
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bikes/*/rent",
                                "/api/v1/bikes/*/return"
                        ).hasAuthority("SCOPE_bike:rental")
                        .anyRequest().denyAll()
                )
                .build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain customerFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/bikes",
                                "/api/v1/bikes/**",
                                "/api/v1/stations",
                                "/api/v1/stations/**"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/bikes",
                                "/api/v1/stations",
                                "/api/v1/bikes/*/maintenance",
                                "/api/v1/bikes/*/maintenance/complete",
                                "/api/v1/bikes/*/retire"
                        ).hasRole("ADMIN")

                        .anyRequest().denyAll()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt
                                .decoder(customerJwtDecoder())
                                .jwtAuthenticationConverter(
                                        customerJwtAuthenticationConverter()
                                )))
                .build();
    }

    @Bean
    public JwtDecoder customerJwtDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(issuerUri)
        );
        return decoder;
    }

    @Bean
    public JwtDecoder serviceJwtDecoder() {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(issuerUri);

        OAuth2TokenValidator<Jwt> audienceValidator =
                new JwtClaimValidator<List<String>>(
                        "aud",
                        audience -> audience != null
                                && audience.contains("bike-service")
                );

        OAuth2TokenValidator<Jwt> tokenKindValidator =
                new JwtClaimValidator<String>(
                        "token_kind",
                        "service"::equals
                );

        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator,
                        tokenKindValidator
                )
        );

        return decoder;
    }

    private JwtAuthenticationConverter customerJwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();

        // Extract roles from the "roles" claim
        authoritiesConverter.setAuthoritiesClaimName("roles");
        // Prepend ROLE_ so it maps properly to hasRole("ADMIN") checks
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }
}
