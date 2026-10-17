package com.cecamed.backend.application.identity.service;

import com.cecamed.backend.application.identity.model.ChangePasswordCommand;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.application.identity.model.UpdateUserCommand;
import com.cecamed.backend.application.identity.model.UserDetailResult;
import com.cecamed.backend.application.identity.model.UserSummaryResult;
import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.PasswordPolicyPort;
import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.UserNotFoundException;
import com.cecamed.backend.domain.identity.exception.UsernameAlreadyExistsException;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class UserAdminService implements UserManagementUseCase {

    private final UserRepositoryPort userRepository;
    private final RefreshTokenRepositoryPort refreshTokenRepository;
    private final SecurityAuditRepositoryPort auditRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final PasswordPolicyPort passwordPolicy;
    private final Clock clock;

    public UserAdminService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            SecurityAuditRepositoryPort auditRepository,
            PasswordEncoderPort passwordEncoder,
            PasswordPolicyPort passwordPolicy,
            Clock clock) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.refreshTokenRepository = Objects.requireNonNull(refreshTokenRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.passwordPolicy = Objects.requireNonNull(passwordPolicy);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public UserDetailResult createUser(CreateUserCommand command) {
        Instant now = clock.instant();
        String normalizedUsername = command.username().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException("El nombre de usuario '" + normalizedUsername + "' ya está registrado.");
        }

        passwordPolicy.validatePassword(command.password());
        String passwordHash = passwordEncoder.encode(command.password());

        UserAccount user = UserAccount.createNew(
                normalizedUsername,
                passwordHash,
                command.fullName(),
                command.role(),
                now
        );

        UserAccount saved = userRepository.save(user);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.USER_CREATED,
                command.performedBy(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Usuario '" + saved.getUsername() + "' creado con rol " + saved.getRole()
        ));

        return toDetailResult(saved);
    }

    @Override
    public UserDetailResult getUser(Long id) {
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));
        return toDetailResult(user);
    }

    @Override
    public PagedResult<UserSummaryResult> listUsers(int page, int size, String search, Role role, Boolean active) {
        PagedResult<UserAccount> paged = userRepository.findAll(page, size, search, role, active);
        var summaries = paged.content().stream()
                .map(u -> new UserSummaryResult(
                        u.getId(),
                        u.getUsername(),
                        u.getFullName(),
                        u.getRole(),
                        u.isActive(),
                        u.getCreatedAt()
                ))
                .toList();

        return new PagedResult<>(summaries, paged.page(), paged.size(), paged.totalElements(), paged.totalPages());
    }

    @Override
    public UserDetailResult updateUser(UpdateUserCommand command) {
        Instant now = clock.instant();
        UserAccount user = userRepository.findById(command.id())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + command.id()));

        Role oldRole = user.getRole();
        user.updateDetails(command.fullName(), command.role(), now);
        UserAccount saved = userRepository.save(user);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.USER_UPDATED,
                command.performedBy(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Usuario '" + saved.getUsername() + "' actualizado. Rol previo: " + oldRole + ", nuevo: " + saved.getRole()
        ));

        return toDetailResult(saved);
    }

    @Override
    public UserDetailResult activateUser(Long id, String performedBy, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));

        user.activate(now);
        UserAccount saved = userRepository.save(user);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.USER_ACTIVATED,
                performedBy,
                ipAddress,
                userAgent,
                now,
                true,
                "Usuario '" + saved.getUsername() + "' activado."
        ));

        return toDetailResult(saved);
    }

    @Override
    public UserDetailResult deactivateUser(Long id, String performedBy, String ipAddress, String userAgent) {
        Instant now = clock.instant();
        UserAccount user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + id));

        user.deactivate(now);
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
        UserAccount saved = userRepository.save(user);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.USER_DEACTIVATED,
                performedBy,
                ipAddress,
                userAgent,
                now,
                true,
                "Usuario '" + saved.getUsername() + "' desactivado y sesiones revocadas."
        ));

        return toDetailResult(saved);
    }

    @Override
    public void resetPassword(ChangePasswordCommand command) {
        Instant now = clock.instant();
        UserAccount user = userRepository.findById(command.id())
                .orElseThrow(() -> new UserNotFoundException("Usuario no encontrado con ID: " + command.id()));

        passwordPolicy.validatePassword(command.newPassword());
        String passwordHash = passwordEncoder.encode(command.newPassword());

        user.changePassword(passwordHash, now);
        refreshTokenRepository.revokeAllForUser(user.getId(), now);
        userRepository.save(user);

        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.PASSWORD_RESET,
                command.performedBy(),
                command.ipAddress(),
                command.userAgent(),
                now,
                true,
                "Contraseña restablecida para usuario '" + user.getUsername() + "' y sesiones revocadas."
        ));
    }

    private UserDetailResult toDetailResult(UserAccount user) {
        return new UserDetailResult(
                user.getId(),
                user.getUsername(),
                user.getFullName(),
                user.getRole(),
                user.isActive(),
                user.getFailedLoginAttempts(),
                user.getLockedUntil(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
