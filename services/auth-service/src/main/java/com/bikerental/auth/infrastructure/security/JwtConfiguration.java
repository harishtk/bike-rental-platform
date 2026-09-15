package com.bikerental.auth.infrastructure.security;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

@Configuration
public class JwtConfiguration {

    @Bean
    public RSAKey rsaKey() {
        try {
            KeyPairGenerator generator =
                    KeyPairGenerator.getInstance("RSA");

            generator.initialize(2048);

            KeyPair keyPair =
                    generator.generateKeyPair();

            RSAPublicKey publicKey =
                    (RSAPublicKey) keyPair.getPublic();

            RSAPrivateKey privateKey =
                    (RSAPrivateKey) keyPair.getPrivate();

            return new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to generate RSA key pair",
                    exception
            );
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            RSAKey rsaKey
    ) {
        return new ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(
                        rsaKey
                )
        );
    }

    @Bean
    public JwtEncoder jwtEncoder(
            JWKSource<SecurityContext> jwkSource
    ) {
        return new NimbusJwtEncoder(jwkSource);
    }
}