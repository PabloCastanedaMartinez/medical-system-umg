package com.cecamed.backend.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");

    @Test
    void createNewInitializesUserCorrectly() {
        UserAccount user = UserAccount.createNew(
                "dr.house",
                "hashedPass",
                "Gregory House",
                Role.MEDICO,
                NOW
        );

        assertThat(user.getUsername()).isEqualTo("dr.house");
        assertThat(user.getFullName()).isEqualTo("Gregory House");
        assertThat(user.getRole()).isEqualTo(Role.MEDICO);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.isLocked(NOW)).isFalse();
        assertThat(user.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void locksAccountWhenMaxAttemptsReached() {
        UserAccount user = UserAccount.createNew("test", "pass", "Test", Role.RECEPCION, NOW);

        // 4 intentos fallidos: no bloquea
        for (int i = 0; i < 4; i++) {
            user.registerFailedLogin(NOW, 5, 15);
            assertThat(user.isLocked(NOW)).isFalse();
        }
        assertThat(user.getFailedLoginAttempts()).isEqualTo(4);

        // 5to intento fallido: bloquea por 15 minutos
        user.registerFailedLogin(NOW, 5, 15);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.isLocked(NOW)).isTrue();
        assertThat(user.getLockedUntil()).isEqualTo(NOW.plusSeconds(15 * 60));

        // 16 minutos después ya no está bloqueada
        assertThat(user.isLocked(NOW.plusSeconds(16 * 60))).isFalse();
    }

    @Test
    void successfulLoginResetsFailedAttemptsAndLock() {
        UserAccount user = UserAccount.createNew("test", "pass", "Test", Role.ADMIN, NOW);
        user.registerFailedLogin(NOW, 5, 15);
        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);

        user.registerSuccessfulLogin(NOW);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void activateAndDeactivateUpdatesStateAndTimestamp() {
        UserAccount user = UserAccount.createNew("test", "pass", "Test", Role.MEDICO, NOW);
        Instant future = NOW.plusSeconds(100);

        user.deactivate(future);
        assertThat(user.isActive()).isFalse();
        assertThat(user.getUpdatedAt()).isEqualTo(future);

        Instant later = future.plusSeconds(100);
        user.activate(later);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    void equalsAndHashCodeBasedOnUsername() {
        UserAccount u1 = UserAccount.createNew("user.same", "pass1", "Name 1", Role.MEDICO, NOW);
        UserAccount u2 = UserAccount.createNew("USER.SAME", "pass2", "Name 2", Role.RECEPCION, NOW);

        assertThat(u1).isEqualTo(u2);
        assertThat(u1.hashCode()).isEqualTo(u2.hashCode());
    }
}
