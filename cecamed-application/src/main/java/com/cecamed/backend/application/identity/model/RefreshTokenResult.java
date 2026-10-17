package com.cecamed.backend.application.identity.model;

public record RefreshTokenResult(
        String accessToken,
        String tokenType,
        int expiresInSeconds,
        String refreshToken
) {}
