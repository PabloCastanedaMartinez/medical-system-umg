package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;
import java.time.Instant;

public record TokenClaims(
        Long userId,
        String username,
        Role role,
        String fullName,
        Instant issuedAt,
        Instant expiresAt
) {}
