package com.cecamed.backend.application.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.cecamed.backend.application.identity.model.CurrentUserResult;
import com.cecamed.backend.application.identity.model.LoginCommand;
import com.cecamed.backend.application.identity.model.LoginResult;
import com.cecamed.backend.application.identity.model.LogoutCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenResult;
import com.cecamed.backend.application.identity.port.out.BruteForcePolicyPort;
import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.identity.port.out.TokenIssuerPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.RefreshToken;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.AccountInactiveException;
import com.cecamed.backend.domain.identity.exception.AccountLockedException;
import com.cecamed.backend.domain.identity.exception.InvalidCredentialsException;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    @Mock private UserRepositoryPort userRepository;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock private SecurityAuditRepositoryPort auditRepository;
    @Mock private TokenIssuerPort tokenIssuer;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private BruteForcePolicyPort bruteForcePolicy;

    private Clock clock;
    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        authService = new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                auditRepository,
                tokenIssuer,
                passwordEncoder,
                bruteForcePolicy,
                clock
        );
    }

    @Test
    void loginSuccessIssuesTokensAndRecordsAudit() {
        UserAccount user = new UserAccount(
                10L, "dr.house", "encoded-pass", "Gregory House", Role.MEDICO, true, 0, null, NOW, NOW
        );
        when(userRepository.findByUsername("dr.house")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("correct-pass", "encoded-pass")).thenReturn(true);
        when(tokenIssuer.issueAccessToken(user, NOW)).thenReturn("jwt.access.token");
        when(tokenIssuer.issueRefreshToken()).thenReturn("raw-refresh-token");
        when(tokenIssuer.hashRefreshToken("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(tokenIssuer.getAccessTokenExpirationMinutes()).thenReturn(15);
        when(tokenIssuer.getRefreshTokenExpirationDays()).thenReturn(7);

        LoginResult result = authService.login(new LoginCommand("dr.house", "correct-pass", "127.0.0.1", "curl"));

        assertThat(result.accessToken()).isEqualTo("jwt.access.token");
        assertThat(result.refreshToken()).isEqualTo("raw-refresh-token");
        assertThat(result.username()).isEqualTo("dr.house");
        assertThat(result.role()).isEqualTo(Role.MEDICO);
        assertThat(result.roleDisplayName()).isEqualTo("Médico");

        verify(bruteForcePolicy).onLoginSuccess(user, NOW);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));

        ArgumentCaptor<SecurityAuditEvent> auditCaptor = ArgumentCaptor.forClass(SecurityAuditEvent.class);
        verify(auditRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getEventType()).isEqualTo(AuditEventType.LOGIN_SUCCESS);
        assertThat(auditCaptor.getValue().isSuccessful()).isTrue();
    }

    @Test
    void loginFailsWhenUserNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginCommand("nonexistent", "secret", "127.0.0.1", "curl")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales incorrectas.");

        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.LOGIN_FAILED));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginFailsWhenPasswordIncorrect() {
        UserAccount user = new UserAccount(
                10L, "dr.house", "encoded-pass", "Gregory House", Role.MEDICO, true, 0, null, NOW, NOW
        );
        when(userRepository.findByUsername("dr.house")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "encoded-pass")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginCommand("dr.house", "wrong-pass", "127.0.0.1", "curl")))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciales incorrectas.");

        verify(bruteForcePolicy).onLoginFailure(user, NOW);
        verify(userRepository).save(user);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.LOGIN_FAILED));
    }

    @Test
    void loginFailsWhenAccountIsLocked() {
        Instant lockedUntil = NOW.plusSeconds(900);
        UserAccount lockedUser = new UserAccount(
                10L, "dr.house", "encoded-pass", "Gregory House", Role.MEDICO, true, 5, lockedUntil, NOW, NOW
        );
        when(userRepository.findByUsername("dr.house")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authService.login(new LoginCommand("dr.house", "any-pass", "127.0.0.1", "curl")))
                .isInstanceOf(AccountLockedException.class)
                .hasMessageContaining("bloqueada temporalmente");

        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.ACCOUNT_LOCKED));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginFailsWhenAccountIsInactive() {
        UserAccount inactiveUser = new UserAccount(
                10L, "inactive.user", "encoded-pass", "Inactive", Role.RECEPCION, false, 0, null, NOW, NOW
        );
        when(userRepository.findByUsername("inactive.user")).thenReturn(Optional.of(inactiveUser));

        assertThatThrownBy(() -> authService.login(new LoginCommand("inactive.user", "pass", "127.0.0.1", "curl")))
                .isInstanceOf(AccountInactiveException.class)
                .hasMessageContaining("desactivada");

        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.LOGIN_FAILED));
    }

    @Test
    void refreshRotatesTokenSuccessfully() {
        RefreshToken existingToken = RefreshToken.createNew(10L, "old-hash", NOW.plusSeconds(3600), NOW);
        UserAccount user = new UserAccount(
                10L, "dr.house", "pass", "Gregory House", Role.MEDICO, true, 0, null, NOW, NOW
        );

        when(tokenIssuer.hashRefreshToken("valid-raw-refresh")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(existingToken));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(tokenIssuer.issueRefreshToken()).thenReturn("new-raw-refresh");
        when(tokenIssuer.hashRefreshToken("new-raw-refresh")).thenReturn("new-hash");
        when(tokenIssuer.getRefreshTokenExpirationDays()).thenReturn(7);
        when(tokenIssuer.getAccessTokenExpirationMinutes()).thenReturn(15);
        when(tokenIssuer.issueAccessToken(user, NOW)).thenReturn("new-access-token");

        RefreshTokenResult result = authService.refresh(new RefreshTokenCommand("valid-raw-refresh", "127.0.0.1", "curl"));

        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-raw-refresh");
        assertThat(existingToken.isRevoked()).isTrue();
        assertThat(existingToken.getReplacedByTokenHash()).isEqualTo("new-hash");

        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.TOKEN_REFRESH && event.isSuccessful()));
    }

    @Test
    void refreshDetectsReuseAndRevokesAllSessions() {
        RefreshToken revokedToken = new RefreshToken(
                1L, 10L, "used-hash", NOW.plusSeconds(3600), NOW, true, NOW.minusSeconds(10), "replacement-hash"
        );

        when(tokenIssuer.hashRefreshToken("compromised-token")).thenReturn("used-hash");
        when(refreshTokenRepository.findByTokenHash("used-hash")).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand("compromised-token", "127.0.0.1", "curl")))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("reutilización indebida");

        verify(refreshTokenRepository).revokeAllForUser(10L, NOW);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.REFRESH_TOKEN_REUSE_DETECTED));
    }

    @Test
    void refreshRejectsExpiredToken() {
        RefreshToken expiredToken = RefreshToken.createNew(10L, "expired-hash", NOW.minusSeconds(10), NOW.minusSeconds(100));

        when(tokenIssuer.hashRefreshToken("expired-raw")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh(new RefreshTokenCommand("expired-raw", "127.0.0.1", "curl")))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("ha expirado");
    }

    @Test
    void logoutRevokesToken() {
        RefreshToken token = RefreshToken.createNew(10L, "logout-hash", NOW.plusSeconds(3600), NOW);
        when(tokenIssuer.hashRefreshToken("logout-token")).thenReturn("logout-hash");
        when(refreshTokenRepository.findByTokenHash("logout-hash")).thenReturn(Optional.of(token));

        authService.logout(new LogoutCommand("logout-token", "dr.house", "127.0.0.1", "curl"));

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.LOGOUT));
    }

    @Test
    void getCurrentUserReturnsUser() {
        UserAccount user = new UserAccount(
                10L, "dr.house", "pass", "Gregory House", Role.MEDICO, true, 0, null, NOW, NOW
        );
        when(userRepository.findByUsername("dr.house")).thenReturn(Optional.of(user));

        CurrentUserResult result = authService.getCurrentUser("dr.house");
        assertThat(result.username()).isEqualTo("dr.house");
        assertThat(result.fullName()).isEqualTo("Gregory House");
        assertThat(result.role()).isEqualTo(Role.MEDICO);
    }
}
