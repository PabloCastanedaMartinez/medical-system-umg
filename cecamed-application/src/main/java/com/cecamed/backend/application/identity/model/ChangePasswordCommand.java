package com.cecamed.backend.application.identity.model;

public record ChangePasswordCommand(
        Long id,
        String newPassword,
        String performedBy,
        String ipAddress,
        String userAgent
) {}
