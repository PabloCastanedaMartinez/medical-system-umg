package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.domain.identity.UserAccount;
import java.time.Instant;

public interface BruteForcePolicyPort {
    int getMaxAttempts();
    int getLockoutDurationMinutes();
    void checkLoginAllowed(UserAccount user, Instant now);
    void onLoginSuccess(UserAccount user, Instant now);
    void onLoginFailure(UserAccount user, Instant now);
}
