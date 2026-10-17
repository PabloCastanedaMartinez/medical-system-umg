package com.cecamed.backend.application.identity.model;

public record LogoutCommand(
        String refreshToken,
        String username,
        String ipAddress,
        String userAgent
) {}
