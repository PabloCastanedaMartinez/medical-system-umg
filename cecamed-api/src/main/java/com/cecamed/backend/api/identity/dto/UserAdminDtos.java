package com.cecamed.backend.api.identity.dto;

import com.cecamed.backend.domain.identity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class UserAdminDtos {

    private UserAdminDtos() {}

    public record CreateUserRequest(
            @NotBlank(message = "El nombre de usuario es obligatorio.")
            @Size(min = 3, max = 80, message = "El nombre de usuario debe tener entre 3 y 80 caracteres.")
            String username,

            @NotBlank(message = "La contraseña es obligatoria.")
            @Size(min = 12, max = 100, message = "La contraseña debe tener al menos 12 caracteres.")
            String password,

            @NotBlank(message = "El nombre completo es obligatorio.")
            @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
            String fullName,

            @NotNull(message = "El rol es obligatorio.")
            Role role
    ) {}

    public record UpdateUserRequest(
            @NotBlank(message = "El nombre completo es obligatorio.")
            @Size(max = 150, message = "El nombre completo no puede superar los 150 caracteres.")
            String fullName,

            @NotNull(message = "El rol es obligatorio.")
            Role role
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "La nueva contraseña es obligatoria.")
            @Size(min = 12, max = 100, message = "La contraseña debe tener al menos 12 caracteres.")
            String newPassword
    ) {}

    public record UserSummaryResponse(
            Long id,
            String username,
            String fullName,
            Role role,
            boolean active,
            Instant createdAt
    ) {}

    public record UserDetailResponse(
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

    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {}
}
