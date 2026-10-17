package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;

public record CreateUserCommand(
        String username,
        String password,
        String fullName,
        Role role,
        String performedBy,
        String ipAddress,
        String userAgent
) {}
