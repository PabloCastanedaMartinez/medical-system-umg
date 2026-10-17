package com.cecamed.backend.application.identity.model;

public record RefreshTokenCommand(
        String refreshToken,
        String ipAddress,
        String userAgent
) {}
