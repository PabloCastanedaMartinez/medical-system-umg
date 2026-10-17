package com.cecamed.backend.application.identity.model;

import com.cecamed.backend.domain.identity.Role;
import java.time.Instant;

public record LoginCommand(
        String username,
        String password,
        String ipAddress,
        String userAgent
) {}
