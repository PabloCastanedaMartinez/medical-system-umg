package com.cecamed.backend.application.identity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cecamed.backend.application.identity.model.ChangePasswordCommand;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.application.identity.model.UpdateUserCommand;
import com.cecamed.backend.application.identity.model.UserDetailResult;
import com.cecamed.backend.application.identity.model.UserSummaryResult;
import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.PasswordPolicyPort;
import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.UserNotFoundException;
import com.cecamed.backend.domain.identity.exception.UsernameAlreadyExistsException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    @Mock private UserRepositoryPort userRepository;
    @Mock private RefreshTokenRepositoryPort refreshTokenRepository;
    @Mock private SecurityAuditRepositoryPort auditRepository;
    @Mock private PasswordEncoderPort passwordEncoder;
    @Mock private PasswordPolicyPort passwordPolicy;

    private Clock clock;
    private UserAdminService adminService;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(NOW, ZoneOffset.UTC);
        adminService = new UserAdminService(
                userRepository,
                refreshTokenRepository,
                auditRepository,
                passwordEncoder,
                passwordPolicy,
                clock
        );
    }

    @Test
    void createUserSuccessfully() {
        when(userRepository.existsByUsername("new.user")).thenReturn(false);
        when(passwordEncoder.encode("SecurePass123!")).thenReturn("hashed-pwd");
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserCommand command = new CreateUserCommand(
                "new.user", "SecurePass123!", "New User", Role.RECEPCION, "admin", "127.0.0.1", "curl"
        );

        UserDetailResult result = adminService.createUser(command);

        assertThat(result.username()).isEqualTo("new.user");
        assertThat(result.fullName()).isEqualTo("New User");
        assertThat(result.role()).isEqualTo(Role.RECEPCION);
        assertThat(result.active()).isTrue();

        verify(passwordPolicy).validatePassword("SecurePass123!");
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.USER_CREATED));
    }

    @Test
    void createUserFailsWhenUsernameAlreadyExists() {
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        CreateUserCommand command = new CreateUserCommand(
                "existing", "SecurePass123!", "Existing User", Role.MEDICO, "admin", "127.0.0.1", "curl"
        );

        assertThatThrownBy(() -> adminService.createUser(command))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("ya está registrado");

        verifyNoInteractions(passwordEncoder);
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserReturnsDetailsOrThrowsNotFound() {
        UserAccount user = new UserAccount(1L, "user1", "hash", "User One", Role.ADMIN, true, 0, null, NOW, NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        UserDetailResult result = adminService.getUser(1L);
        assertThat(result.username()).isEqualTo("user1");

        assertThatThrownBy(() -> adminService.getUser(999L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void listUsersReturnsPagedResults() {
        UserAccount u1 = new UserAccount(1L, "admin", "hash", "Admin", Role.ADMIN, true, 0, null, NOW, NOW);
        UserAccount u2 = new UserAccount(2L, "doc", "hash", "Doctor", Role.MEDICO, true, 0, null, NOW, NOW);

        when(userRepository.findAll(0, 10, null, null, null))
                .thenReturn(new PagedResult<>(List.of(u1, u2), 0, 10, 2, 1));

        PagedResult<UserSummaryResult> paged = adminService.listUsers(0, 10, null, null, null);
        assertThat(paged.totalElements()).isEqualTo(2);
        assertThat(paged.content()).hasSize(2);
        assertThat(paged.content().get(0).username()).isEqualTo("admin");
    }

    @Test
    void updateUserUpdatesDetailsAndAudits() {
        UserAccount user = new UserAccount(1L, "user1", "hash", "Old Name", Role.RECEPCION, true, 0, null, NOW, NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDetailResult updated = adminService.updateUser(
                new UpdateUserCommand(1L, "New Name", Role.MEDICO, "admin", "127.0.0.1", "curl")
        );

        assertThat(updated.fullName()).isEqualTo("New Name");
        assertThat(updated.role()).isEqualTo(Role.MEDICO);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.USER_UPDATED));
    }

    @Test
    void deactivateUserDeactivatesAndRevokesSessions() {
        UserAccount user = new UserAccount(1L, "user1", "hash", "User 1", Role.MEDICO, true, 0, null, NOW, NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserDetailResult result = adminService.deactivateUser(1L, "admin", "127.0.0.1", "curl");

        assertThat(result.active()).isFalse();
        verify(refreshTokenRepository).revokeAllForUser(1L, NOW);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.USER_DEACTIVATED));
    }

    @Test
    void resetPasswordValidatesPolicyEncodesAndRevokesSessions() {
        UserAccount user = new UserAccount(1L, "user1", "old-hash", "User 1", Role.MEDICO, true, 0, null, NOW, NOW);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NewSecretPassword123!")).thenReturn("new-hash");

        adminService.resetPassword(new ChangePasswordCommand(1L, "NewSecretPassword123!", "admin", "127.0.0.1", "curl"));

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        verify(passwordPolicy).validatePassword("NewSecretPassword123!");
        verify(refreshTokenRepository).revokeAllForUser(1L, NOW);
        verify(userRepository).save(user);
        verify(auditRepository).save(argThat(event -> event.getEventType() == AuditEventType.PASSWORD_RESET));
    }
}
