package com.bikerental.auth.domain.user;

import java.util.Optional;

public interface AuthUserRepository {

    AuthUser save(AuthUser user);

    Optional<AuthUser> findByUsername(String username);

    boolean existsByUsername(String username);
}