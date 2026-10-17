package com.cecamed.backend.infrastructure.security.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cecamed.backend.application.identity.model.TokenClaims;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class JwtTokenServiceTest {

    private static final String SECRET_KEY = "test-secret-key-that-is-at-least-256-bits-long-32-chars!";
    private static final String OTHER_SECRET_KEY = "other-secret-key-that-is-also-at-least-256-bits-long-32!";
    private static final Instant FIXED_NOW = Instant.parse("2026-10-17T10:00:00Z");

    private MutableClock clock;
    private JsonMapper jsonMapper;
    private JwtTokenService tokenService;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(FIXED_NOW);
        jsonMapper = JsonMapper.builder().build();
        tokenService = new JwtTokenService(SECRET_KEY, 15, 7, clock, jsonMapper);
    }

    @Test
    void issuesAndValidatesAccessTokenSuccessfully() {
        UserAccount user = new UserAccount(
                42L, "doctor.who", "hashed", "Dr. Who", Role.MEDICO, true, 0, null, FIXED_NOW, FIXED_NOW
        );

        String token = tokenService.issueAccessToken(user, FIXED_NOW);
        assertThat(token).isNotBlank();

        TokenClaims claims = tokenService.validateAccessToken(token);
        assertThat(claims.userId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("doctor.who");
        assertThat(claims.fullName()).isEqualTo("Dr. Who");
        assertThat(claims.role()).isEqualTo(Role.MEDICO);
        assertThat(claims.issuedAt()).isEqualTo(FIXED_NOW);
        assertThat(claims.expiresAt()).isEqualTo(FIXED_NOW.plusSeconds(15 * 60));
    }

    @Test
    void rejectsExpiredAccessToken() {
        UserAccount user = new UserAccount(
                1L, "exp.user", "hashed", "Exp User", Role.RECEPCION, true, 0, null, FIXED_NOW, FIXED_NOW
        );

        String token = tokenService.issueAccessToken(user, FIXED_NOW);

        // Avanzar el reloj 16 minutos (expiración es 15)
        clock.setInstant(FIXED_NOW.plusSeconds(16 * 60));

        assertThatThrownBy(() -> tokenService.validateAccessToken(token))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("ha expirado");
    }

    @Test
    void rejectsTamperedTokenPayload() {
        UserAccount user = new UserAccount(
                1L, "user1", "hashed", "User One", Role.MEDICO, true, 0, null, FIXED_NOW, FIXED_NOW
        );

        String token = tokenService.issueAccessToken(user, FIXED_NOW);
        String[] parts = token.split("\\.");
        // Modificar el payload
        String tamperedToken = parts[0] + ".eyAidGFtcGVyZWQiOiB0cnVlIH0." + parts[2];

        assertThatThrownBy(() -> tokenService.validateAccessToken(tamperedToken))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Firma del token no válida");
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        UserAccount user = new UserAccount(
                1L, "user1", "hashed", "User One", Role.MEDICO, true, 0, null, FIXED_NOW, FIXED_NOW
        );

        JwtTokenService otherService = new JwtTokenService(OTHER_SECRET_KEY, 15, 7, clock, jsonMapper);
        String tokenFromOther = otherService.issueAccessToken(user, FIXED_NOW);

        assertThatThrownBy(() -> tokenService.validateAccessToken(tokenFromOther))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Firma del token no válida");
    }

    @Test
    void rejectsMalformedToken() {
        assertThatThrownBy(() -> tokenService.validateAccessToken("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);

        assertThatThrownBy(() -> tokenService.validateAccessToken(""))
                .isInstanceOf(InvalidTokenException.class);

        assertThatThrownBy(() -> tokenService.validateAccessToken(null))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void issuesOpaqueRefreshTokenAndComputesSha256Hash() {
        String rawToken = tokenService.issueRefreshToken();
        assertThat(rawToken).isNotBlank().hasSizeGreaterThan(20);

        String hash = tokenService.hashRefreshToken(rawToken);
        assertThat(hash).isNotBlank().hasSize(64); // SHA-256 en hexadecimal son 64 caracteres

        // Mismo token genera mismo hash
        assertThat(tokenService.hashRefreshToken(rawToken)).isEqualTo(hash);
    }

    private static class MutableClock extends Clock {
        private Instant currentInstant;

        MutableClock(Instant initial) {
            this.currentInstant = initial;
        }

        void setInstant(Instant instant) {
            this.currentInstant = instant;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }
}
