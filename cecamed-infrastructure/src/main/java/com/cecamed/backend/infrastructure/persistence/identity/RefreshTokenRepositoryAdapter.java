package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.domain.identity.RefreshToken;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenSpringDataRepository repository;

    public RefreshTokenRepositoryAdapter(RefreshTokenSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) return Optional.empty();
        return repository.findByTokenHash(tokenHash).map(RefreshTokenJpaEntity::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        RefreshTokenJpaEntity entity = RefreshTokenJpaEntity.fromDomain(refreshToken);
        return repository.save(entity).toDomain();
    }

    @Override
    public void revokeAllForUser(Long userId, Instant now) {
        if (userId != null) {
            repository.revokeAllByUserId(userId, now);
        }
    }
}
