package com.bikerental.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfiguration {

    @Bean
    SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http
    ) {
        return http
                .csrf(
                        ServerHttpSecurity.CsrfSpec::disable
                )
                .authorizeExchange(exchange ->
                        exchange
                                .pathMatchers(
                                        "/api/v1/auth/register",
                                        "/api/v1/auth/login",
                                        "/actuator/**"
                                )
                                .permitAll()

                                .anyExchange()
                                .authenticated()
                )
                .oauth2ResourceServer(
                        oauth ->
                                oauth.jwt(
                                        Customizer.withDefaults()
                                )
                )
                .build();
    }
}