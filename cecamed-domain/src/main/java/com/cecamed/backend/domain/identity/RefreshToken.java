package com.cecamed.backend.domain.identity;

import java.time.Instant;
import java.util.Objects;

public class RefreshToken {
    private final Long id;
    private final Long userId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private boolean revoked;
    private Instant revokedAt;
    private String replacedByTokenHash;

    public RefreshToken(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            boolean revoked,
            Instant revokedAt,
            String replacedByTokenHash) {
        this.id = id;
        this.userId = Objects.requireNonNull(userId, "userId no puede ser nulo");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash no puede ser nulo");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt no puede ser nulo");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt no puede ser nulo");
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public static RefreshToken createNew(Long userId, String tokenHash, Instant expiresAt, Instant createdAt) {
        return new RefreshToken(null, userId, tokenHash, expiresAt, createdAt, false, null, null);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    public boolean isActive(Instant now) {
        return !revoked && !isExpired(now);
    }

    public void revoke(Instant now, String replacementTokenHash) {
        this.revoked = true;
        this.revokedAt = now;
        this.replacedByTokenHash = replacementTokenHash;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public String getReplacedByTokenHash() {
        return replacedByTokenHash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshToken that = (RefreshToken) o;
        return Objects.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }
}
