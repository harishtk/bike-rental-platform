package com.bikerental.auth.infrastructure.config;

import com.bikerental.auth.domain.user.AuthUser;
import com.bikerental.auth.domain.user.AuthUserRepository;
import com.bikerental.auth.domain.user.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Configuration
public class E2eAdminConfiguration {

    @Bean
    ApplicationRunner createE2eAdmin(
            AuthUserRepository repository,
            PasswordEncoder passwordEncoder,
            Clock clock
    ) {
        return args -> {
            String username = "e2e-admin";

            var existing = repository.findByUsername(username);

            if (existing.isPresent()) {
                if (existing.get().getRole() != UserRole.ADMIN) {
                    throw new IllegalStateException(
                            "E2E admin fixture has an unexpected role"
                    );
                }
                return;
            }

            Instant now = clock.instant();

            repository.save(AuthUser.restore(
                    UUID.randomUUID(),
                    username,
                    passwordEncoder.encode("e2e-admin-test-only"),
                    UserRole.ADMIN,
                    now,
                    now
            ));
        };
    }
}