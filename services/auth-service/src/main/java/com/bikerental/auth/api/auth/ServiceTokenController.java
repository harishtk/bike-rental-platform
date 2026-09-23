package com.bikerental.auth.api.auth;

import com.bikerental.auth.application.auth.ServiceTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/auth")
public class ServiceTokenController {

    private final ServiceTokenService serviceTokenService;

    @PostMapping("/service-token")
    public ResponseEntity<TokenResponse> issue(
            @Valid @RequestBody ServiceTokenRequest serviceTokenRequest
    ) {
        TokenResponse token = serviceTokenService.issue(
                serviceTokenRequest.clientId(),
                serviceTokenRequest.clientSecret()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(token);

    }
}
