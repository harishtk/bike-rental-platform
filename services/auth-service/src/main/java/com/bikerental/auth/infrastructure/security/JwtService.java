package com.bikerental.auth.infrastructure.security;

import com.bikerental.auth.domain.user.AuthUser;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final RSAKey rsaKey;
    private final Clock clock;

    @Value("${auth.jwt.issuer}")
    private String issuer;

    @Value("${auth.jwt.access-token-ttl}")
    private long accessTokenTtl;

    public String createAccessToken(
            AuthUser user
    ) {
        Instant now =
                Instant.now(clock);

        Instant expiresAt =
                now.plusSeconds(accessTokenTtl);

        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .issuedAt(now)
                        .expiresAt(expiresAt)
                        .subject(
                                user.getId().toString()
                        )
                        .claim(
                                "username",
                                user.getUsername()
                        )
                        .claim(
                                "roles",
                                List.of(
                                        user.getRole().name()
                                )
                        )
                        .build();

        JwsHeader header =
                JwsHeader
                        .with(
                                SignatureAlgorithm.RS256
                        )
                        .keyId(
                                rsaKey.getKeyID()
                        )
                        .build();

        return jwtEncoder
                .encode(
                        JwtEncoderParameters.from(
                                header,
                                claims
                        )
                )
                .getTokenValue();
    }

    public long getAccessTokenTtl() {
        return accessTokenTtl;
    }
}