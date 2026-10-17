package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;
import java.time.Instant;

public record UserDetailResult(
        Long id,
        String username,
        String fullName,
        Role role,
        boolean active,
        int failedLoginAttempts,
        Instant lockedUntil,
        Instant createdAt,
        Instant updatedAt
) {}
