package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.domain.identity.RefreshToken;
import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepositoryPort {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    RefreshToken save(RefreshToken refreshToken);
    void revokeAllForUser(Long userId, Instant now);
}
