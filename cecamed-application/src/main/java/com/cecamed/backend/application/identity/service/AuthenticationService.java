package com.cecamed.backend.application.identity.service;

import com.cecamed.backend.application.identity.model.CurrentUserResult;
import com.cecamed.backend.application.identity.model.LoginCommand;
import com.cecamed.backend.application.identity.model.LoginResult;
import com.cecamed.backend.application.identity.model.LogoutCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenResult;
import com.cecamed.backend.application.identity.port.in.GetCurrentUserUseCase;
import com.cecamed.backend.application.identity.port.in.LoginUseCase;
import com.cecamed.backend.application.identity.port.in.LogoutUseCase;
import com.cecamed.backend.application.identity.port.in.RefreshTokenUseCase;
import com.cecamed.backend.application.identity.port.out.BruteForcePolicyPort;
import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.identity.port.out.TokenIssuerPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.RefreshToken;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.AccountInactiveException;
import com.cecamed.backend.domain.identity.exception.AccountLockedException;
import com.cecamed.backend.domain.identity.exception.InvalidCredentialsException;
import com.cecamed.backend.domain.identity.exception.InvalidTokenException;
import com.cecamed.backend.domain.identity.exception.TokenExpiredException;
import com.cecamed.backend.domain.identity.exception.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class AuthenticationService implements LoginUseCase, RefreshTokenUseCase, LogoutUseCase, GetCurrentUserUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final SecurityAuditRepositoryPort auditRepository;
    private final TokenIssuerPort tokenIssuer;
    private final PasswordEncoderPort passwordEncoder;
    private final BruteForcePolicyPort bruteForcePolicy;
    private final Clock clock;

    public AuthenticationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            SecurityAuditRepositoryPort auditRepository,
            TokenIssuerPort tokenIssuer,
            PasswordEncoderPort passwordEncoder,
            BruteForcePolicyPort bruteForcePolicy,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.tokenIssuer = Objects.requireNonNull(tokenIssuer);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.bruteForcePolicy = Objects.requireNonNull(bruteForcePolicy);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public LoginResult login(LoginCommand command) {
        Instant now = clock.instant();
        String normalizedUsername = command.username() != null ? command.username().trim().toLowerCase() : "";

        var userOpt = userRepository.findByUsername(normalizedUsername);
        if (userOpt.isEmpty()) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.LOGIN_FAILED,
                    normalizedUsername,
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Usuario no existe"
            ));
            throw new InvalidCredentialsException("Credenciales incorrectas.");
        }

        UserAccount user = userOpt.get();

        if (user.isLocked(now)) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.ACCOUNT_LOCKED,
                    user.getUsername(),
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Intento en cuenta bloqueada temporalmente hasta " + user.getLockedUntil()
            ));
            throw new AccountLockedException("La cuenta se encuentra bloqueada temporalmente por múltiples intentos fallidos.", user.getLockedUntil());
        }

        if (!user.isActive()) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.LOGIN_FAILED,
                    user.getUsername(),
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Intento de acceso a cuenta inactiva"
            ));
            throw new AccountInactiveException("La cuenta de usuario está desactivada.");
        }

        if (!passwordEncoder.matches(command.password(), user.getPasswordHash())) {
            bruteForcePolicy.onLoginFailure(user, now);
            userRepository.save(user);

            if (user.isLocked(now)) {
                auditRepository.save(SecurityAuditEvent.of(
                        AuditEventType.ACCOUNT_LOCKED,
                        user.getUsername(),
                        command.ipAddress(),
                        command.userAgent(),
                        now,
                        false,
                        "Cuenta bloqueada tras " + user.getFailedLoginAttempts() + " intentos fallidos"
                ));
                throw new AccountLockedException("La cuenta ha sido bloqueada temporalmente por superar el límite de intentos.", user.getLockedUntil());
            }

            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.LOGIN_FAILED,
                    user.getUsername(),
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Contraseña incorrecta. Intento " + user.getFailedLoginAttempts()
            ));
            throw new InvalidCredentialsException("Credenciales incorrectas.");
        }

        bruteForcePolicy.onLoginSuccess(user, now);
        userRepository.save(user);

        String accessToken = tokenIssuer.issueAccessToken(user, now);
        String rawRefreshToken = tokenIssuer.issueRefreshToken();
        String tokenHash = tokenIssuer.hashRefreshToken(rawRefreshToken);
        Instant refreshExpiresAt = now.plus(tokenIssuer.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        RefreshToken refreshToken = RefreshToken.createNew(user.getId(), tokenHash, refreshExpiresAt, now);
        refreshTokenRepository.save(refreshToken);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.LOGIN_SUCCESS,
                user.getUsername(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Inicio de sesión exitoso con rol " + user.getRole()
        ));

        int expiresInSeconds = tokenIssuer.getAccessTokenExpirationMinutes() * 60;
        return new LoginResult(
                accessToken,
                "Bearer",
                expiresInSeconds,
                rawRefreshToken,
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getRole().getDisplayName(),
                now
        );
    }

    @Override
    public RefreshTokenResult refresh(RefreshTokenCommand command) {
        Instant now = clock.instant();
        String tokenHash = tokenIssuer.hashRefreshToken(command.refreshToken());

        var tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        if (tokenOpt.isEmpty()) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.TOKEN_REFRESH,
                    null,
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Token de actualización no encontrado"
            ));
            throw new InvalidTokenException("Token de actualización inválido.");
        }

        RefreshToken token = tokenOpt.get();

        if (token.isRevoked()) {
            // Detección de reutilización indebida de token (posible robo de sesión)
            refreshTokenRepository.revokeAllForUser(token.getUserId(), now);
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.REFRESH_TOKEN_REUSE_DETECTED,
                    null,
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Reutilización indebida detectada para usuario ID " + token.getUserId() + ". Todas las sesiones revocadas."
            ));
            throw new InvalidTokenException("Se detectó una reutilización indebida del token de actualización. Se invalidaron todas las sesiones por seguridad.");
        }

        if (token.isExpired(now)) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.TOKEN_REFRESH,
                    null,
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Token de actualización expirado"
            ));
            throw new TokenExpiredException("El token de actualización ha expirado.");
        }

        UserAccount user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado."));

        if (!user.isActive()) {
            auditRepository.save(SecurityAuditEvent.of(
                    AuditEventType.TOKEN_REFRESH,
                    user.getUsername(),
                    command.ipAddress(),
                    command.userAgent(),
                    now,
                    false,
                    "Intento de refresco con cuenta desactivada"
            ));
            throw new AccountInactiveException("La cuenta de usuario está desactivada.");
        }

        // Rotación de token: generar nuevo par
        String newRawRefreshToken = tokenIssuer.issueRefreshToken();
        String newHash = tokenIssuer.hashRefreshToken(newRawRefreshToken);
        Instant newRefreshExpiresAt = now.plus(tokenIssuer.getRefreshTokenExpirationDays(), ChronoUnit.DAYS);

        token.revoke(now, newHash);
        refreshTokenRepository.save(token);

        RefreshToken newRefreshToken = RefreshToken.createNew(user.getId(), newHash, newRefreshExpiresAt, now);
        refreshTokenRepository.save(newRefreshToken);

        String newAccessToken = tokenIssuer.issueAccessToken(user, now);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.TOKEN_REFRESH,
                user.getUsername(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Rotación de refresh token exitosa"
        ));

        int expiresInSeconds = tokenIssuer.getAccessTokenExpirationMinutes() * 60;
        return new RefreshTokenResult(newAccessToken, "Bearer", expiresInSeconds, newRawRefreshToken);
    }

    @Override
    public void logout(LogoutCommand command) {
        Instant now = clock.instant();
        if (command.refreshToken() != null && !command.refreshToken().isBlank()) {
            String tokenHash = tokenIssuer.hashRefreshToken(command.refreshToken());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
                token.revoke(now, null);
                refreshTokenRepository.save(token);
            });
        }

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.LOGOUT,
                command.username(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Cierre de sesión"
        ));
    }

    @Override
    public CurrentUserResult getCurrentUser(String username) {
        Instant now = clock.instant();
        String normalizedUsername = username != null ? username.trim().toLowerCase() : "";
        UserAccount user = userRepository.findByUsername(normalizedUsername)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado: " + normalizedUsername));

        if (!user.isActive()) {
            throw new AccountInactiveException("La cuenta de usuario está desactivada.");
        }

        return new CurrentUserResult(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.getRole().getDisplayName(),
                now
        );
    }
}
