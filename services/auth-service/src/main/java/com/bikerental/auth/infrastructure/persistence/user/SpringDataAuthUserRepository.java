package com.bikerental.auth.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SpringDataAuthUserRepository
        extends JpaRepository<AuthUserEntity, UUID> {

    Optional<AuthUserEntity> findByUsername(
            String username
    );

    boolean existsByUsername(
            String username
    );
}