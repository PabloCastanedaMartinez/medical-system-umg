package com.cecamed.backend.infrastructure.persistence.identity;

import com.cecamed.backend.domain.identity.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "refresh_tokens", schema = "cecamed")
public class RefreshTokenJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_token_hash", length = 64)
    private String replacedByTokenHash;

    protected RefreshTokenJpaEntity() {}

    public RefreshTokenJpaEntity(
            Long id,
            Long userId,
            String tokenHash,
            Instant expiresAt,
            Instant createdAt,
            boolean revoked,
            Instant revokedAt,
            String replacedByTokenHash) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revoked = revoked;
        this.revokedAt = revokedAt;
        this.replacedByTokenHash = replacedByTokenHash;
    }

    public static RefreshTokenJpaEntity fromDomain(RefreshToken domain) {
        return new RefreshTokenJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getTokenHash(),
                domain.getExpiresAt(),
                domain.getCreatedAt(),
                domain.isRevoked(),
                domain.getRevokedAt(),
                domain.getReplacedByTokenHash()
        );
    }

    public RefreshToken toDomain() {
        return new RefreshToken(
                this.id,
                this.userId,
                this.tokenHash,
                this.expiresAt,
                this.createdAt,
                this.revoked,
                this.revokedAt,
                this.replacedByTokenHash
        );
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isRevoked() { return revoked; }
    public Instant getRevokedAt() { return revokedAt; }
    public String getReplacedByTokenHash() { return replacedByTokenHash; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RefreshTokenJpaEntity that = (RefreshTokenJpaEntity) o;
        return Objects.equals(tokenHash, that.tokenHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenHash);
    }
}
