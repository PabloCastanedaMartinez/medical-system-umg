package com.cecamed.backend.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    @Test
    void createNewAndValidateActiveStatus() {
        RefreshToken token = RefreshToken.createNew(10L, "hash123", NOW.plusSeconds(3600), NOW);

        assertThat(token.getUserId()).isEqualTo(10L);
        assertThat(token.getTokenHash()).isEqualTo("hash123");
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.isExpired(NOW)).isFalse();
        assertThat(token.isActive(NOW)).isTrue();

        // Expira 3601 segundos después
        assertThat(token.isExpired(NOW.plusSeconds(3601))).isTrue();
        assertThat(token.isActive(NOW.plusSeconds(3601))).isFalse();
    }

    @Test
    void revokeMarksTokenAsRevokedWithReplacement() {
        RefreshToken token = RefreshToken.createNew(10L, "hash123", NOW.plusSeconds(3600), NOW);

        token.revoke(NOW, "newHash456");

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW);
        assertThat(token.getReplacedByTokenHash()).isEqualTo("newHash456");
        assertThat(token.isActive(NOW)).isFalse();
    }
}
