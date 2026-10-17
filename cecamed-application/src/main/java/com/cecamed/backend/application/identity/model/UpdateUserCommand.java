package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;

public record UpdateUserCommand(
        Long id,
        String fullName,
        Role role,
        String performedBy,
        String ipAddress,
        String userAgent
) {}
