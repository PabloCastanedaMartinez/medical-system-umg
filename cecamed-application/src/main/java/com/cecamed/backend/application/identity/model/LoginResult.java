package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;
import java.time.Instant;

public record LoginResult(
        String accessToken,
        String tokenType,
        int expiresInSeconds,
        String refreshToken,
        String username,
        String fullName,
        Role role,
        String roleDisplayName,
        Instant loginTime
) {}
