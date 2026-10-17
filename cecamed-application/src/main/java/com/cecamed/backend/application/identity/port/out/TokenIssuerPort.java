package com.cecamed.backend.application.identity.port.out;

import com.cecamed.backend.domain.identity.UserAccount;
import java.time.Instant;

public interface TokenIssuerPort {
    String issueAccessToken(UserAccount user, Instant now);
    String issueRefreshToken();
    String hashRefreshToken(String rawRefreshToken);
    int getAccessTokenExpirationMinutes();
    int getRefreshTokenExpirationDays();
}
