package com.bikerental.auth.infrastructure.persistence.user;

import com.bikerental.auth.domain.user.AuthUser;
import com.bikerental.auth.domain.user.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthUserPersistenceAdapter
        implements AuthUserRepository {

    private final SpringDataAuthUserRepository repository;

    @Override
    public AuthUser save(AuthUser user) {
        AuthUserEntity entity =
                new AuthUserEntity(
                        user.getId(),
                        user.getUsername(),
                        user.getPassword(),
                        user.getRole(),
                        user.getCreatedAt(),
                        user.getUpdatedAt()
                );

        return toDomain(
                repository.save(entity)
        );
    }

    @Override
    public Optional<AuthUser> findByUsername(
            String username
    ) {
        return repository
                .findByUsername(username)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByUsername(
            String username
    ) {
        return repository.existsByUsername(username);
    }

    private AuthUser toDomain(
            AuthUserEntity entity
    ) {
        return AuthUser.restore(
                entity.getId(),
                entity.getUsername(),
                entity.getPassword(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}