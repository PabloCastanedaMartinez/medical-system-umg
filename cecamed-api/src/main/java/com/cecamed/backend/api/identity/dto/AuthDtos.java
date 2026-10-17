package com.cecamed.backend.api.identity.dto;

import com.cecamed.backend.domain.identity.Role;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public final class AuthDtos {

    private AuthDtos() {}

    public record LoginRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio.")
            String username,

            @NotBlank(message = "La contraseña es obligatoria.")
            String password
    ) {}

    public record LoginResponse(
            String accessToken,
            String tokenType,
            int expiresInSeconds,
            String refreshToken,
            String username,
            String fullName,
            Role role,
            String roleDisplayName,
            Instant loginTime
    ) {}

    public record RefreshTokenRequest(
            @NotBlank(message = "El token de actualización es obligatorio.")
            String refreshToken
    ) {}

    public record RefreshTokenResponse(
            String accessToken,
            String tokenType,
            int expiresInSeconds,
            String refreshToken
    ) {}

    public record LogoutRequest(
            String refreshToken
    ) {}

    public record CurrentUserResponse(
            String username,
            String fullName,
            Role role,
            String roleDisplayName,
            Instant loginTime
    ) {}
}
