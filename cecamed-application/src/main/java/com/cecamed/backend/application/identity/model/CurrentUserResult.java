package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;
import java.time.Instant;

public record CurrentUserResult(
        Long id,
        String username,
        String fullName,
        Role role,
        String roleDisplayName,
        Instant loginTime
) {}
