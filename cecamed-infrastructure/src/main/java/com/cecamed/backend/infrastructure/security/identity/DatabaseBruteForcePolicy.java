package com.cecamed.backend.infrastructure.security.identity;

import com.cecamed.backend.application.identity.port.out.BruteForcePolicyPort;
import com.cecamed.backend.domain.identity.UserAccount;
import com.cecamed.backend.domain.identity.exception.AccountLockedException;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseBruteForcePolicy implements BruteForcePolicyPort {

    private final int maxAttempts;
    private final int lockoutDurationMinutes;

    public DatabaseBruteForcePolicy(
            @Value("${cecamed.security.brute-force.max-attempts:5}") int maxAttempts,
            @Value("${cecamed.security.brute-force.lockout-duration-minutes:15}") int lockoutDurationMinutes) {
        this.maxAttempts = maxAttempts;
        this.lockoutDurationMinutes = lockoutDurationMinutes;
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public int getLockoutDurationMinutes() {
        return lockoutDurationMinutes;
    }

    @Override
    public void checkLoginAllowed(UserAccount user, Instant now) {
        if (user.isLocked(now)) {
            throw new AccountLockedException(
                    "La cuenta se encuentra bloqueada temporalmente hasta " + user.getLockedUntil(),
                    user.getLockedUntil()
            );
        }
    }

    @Override
    public void onLoginSuccess(UserAccount user, Instant now) {
        user.registerSuccessfulLogin(now);
    }

    @Override
    public void onLoginFailure(UserAccount user, Instant now) {
        user.registerFailedLogin(now, maxAttempts, lockoutDurationMinutes);
    }
}
