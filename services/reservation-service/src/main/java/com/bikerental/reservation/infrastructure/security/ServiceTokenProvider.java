package com.bikerental.reservation.infrastructure.security;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class ServiceTokenProvider {

    private static final long REFRESH_MARGIN_SECONDS = 30;

    private final RestClient authClient;
    private final Clock clock;
    private final String clientId;
    private final String clientSecret;

    private String cachedToken;
    private Instant refreshAt = Instant.MIN;

    public ServiceTokenProvider(
            RestClient.Builder builder,
            Clock clock,
            @Value("${clients.auth-service.url}") String authUrl,
            @Value("${service-auth.client-id}") String clientId,
            @Value("${service-auth.client-secret}") String clientSecret
    ) {
        if (clientId.isBlank() || clientSecret.isBlank()) {
            throw new IllegalArgumentException("Service credentials must not be blank");
        }

        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(3));

        this.authClient = builder.clone()
                .baseUrl(authUrl)
                .requestFactory(requestFactory)
                .build();
        this.clock = clock;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String accessToken() {
        Instant now = clock.instant();

        if (cachedToken != null && now.isBefore(refreshAt)) {
            return cachedToken;
        }

        Instant requestedAt = now;
        TokenResponse response = null;

        try {
            response = authClient.post()
                    .uri("/internal/auth/service-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new TokenRequest(clientId, clientSecret))
                    .retrieve()
                    .onStatus(
                            HttpStatusCode::isError,
                            ((request, result) -> {
                                throw new IllegalStateException(
                                        "Service authentication failed: HTTP " +
                                                result.getStatusCode().value()
                                );
                            })
                    )
                    .body(TokenResponse.class);
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "Service authentication is unavailable"
            );
        }

        if (response == null
            || response.accessToken() == null
            || response.accessToken().isBlank()
            || !"Bearer".equalsIgnoreCase(response.tokenType())
            || response.expiresIn() <= REFRESH_MARGIN_SECONDS) {
            throw new IllegalStateException(
                    "Invalid service-token response " + response
            );
        }

        Instant nextRefreshAt = requestedAt.plusSeconds(
                response.expiresIn() - REFRESH_MARGIN_SECONDS
        );

        if (!clock.instant().isBefore(nextRefreshAt)) {
            throw new IllegalStateException(
                    "Service token has insufficient remaining lifetime"
            );
        }

        cachedToken = response.accessToken();
        refreshAt = nextRefreshAt;

        return cachedToken;
    }

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn
    ) {

//        @Override
//        @NotNull
//        public String toString() {
//            return "TokenResponse[token redacted]";
//        }
    }

    public record TokenRequest(
            String clientId,
            String clientSecret
    ) {
//        @Override
//        @NotNull
//        public String toString() {
//            return "TokenRequest[credentials redacted]";
//        }
    }

}
