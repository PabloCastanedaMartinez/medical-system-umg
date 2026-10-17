package com.cecamed.backend.api.identity;

import com.cecamed.backend.api.identity.dto.UserAdminDtos;
import com.cecamed.backend.application.identity.model.ChangePasswordCommand;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.model.PagedResult;
import com.cecamed.backend.application.identity.model.UpdateUserCommand;
import com.cecamed.backend.application.identity.model.UserDetailResult;
import com.cecamed.backend.application.identity.model.UserSummaryResult;
import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.domain.identity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administración de Usuarios", description = "Gestión de cuentas, roles, estados y contraseñas (exclusivo para administradores).")
public class UserAdminController {

    private final UserManagementUseCase userManagementUseCase;

    public UserAdminController(UserManagementUseCase userManagementUseCase) {
        this.userManagementUseCase = Objects.requireNonNull(userManagementUseCase);
    }

    @PostMapping
    @Operation(summary = "Crear un nuevo usuario del sistema")
    public ResponseEntity<UserAdminDtos.UserDetailResponse> createUser(
            @Valid @RequestBody UserAdminDtos.CreateUserRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        UserDetailResult result = userManagementUseCase.createUser(new CreateUserCommand(
                request.username(),
                request.password(),
                request.fullName(),
                request.role(),
                authentication.getName(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return ResponseEntity
                .created(URI.create("/api/v1/users/" + result.id()))
                .body(toDetailResponse(result));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios con paginación y filtros")
    public UserAdminDtos.PageResponse<UserAdminDtos.UserSummaryResponse> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean active) {
        PagedResult<UserSummaryResult> paged = userManagementUseCase.listUsers(page, size, search, role, active);
        var content = paged.content().stream()
                .map(u -> new UserAdminDtos.UserSummaryResponse(
                        u.id(),
                        u.username(),
                        u.fullName(),
                        u.role(),
                        u.active(),
                        u.createdAt()
                ))
                .toList();

        return new UserAdminDtos.PageResponse<>(
                content,
                paged.page(),
                paged.size(),
                paged.totalElements(),
                paged.totalPages()
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar el detalle de un usuario")
    public UserAdminDtos.UserDetailResponse getUser(@PathVariable Long id) {
        return toDetailResponse(userManagementUseCase.getUser(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar nombre completo y rol de un usuario")
    public UserAdminDtos.UserDetailResponse updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserAdminDtos.UpdateUserRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        UserDetailResult result = userManagementUseCase.updateUser(new UpdateUserCommand(
                id,
                request.fullName(),
                request.role(),
                authentication.getName(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return toDetailResponse(result);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Activar la cuenta de un usuario")
    public UserAdminDtos.UserDetailResponse activateUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        UserDetailResult result = userManagementUseCase.activateUser(
                id,
                authentication.getName(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return toDetailResponse(result);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desactivar la cuenta de un usuario y revocar sus sesiones")
    public UserAdminDtos.UserDetailResponse deactivateUser(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        UserDetailResult result = userManagementUseCase.deactivateUser(
                id,
                authentication.getName(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        return toDetailResponse(result);
    }

    @PostMapping("/{id}/reset-password")
    @Operation(summary = "Restablecer la contraseña de un usuario y revocar sus sesiones")
    public ResponseEntity<Void> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody UserAdminDtos.ResetPasswordRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        userManagementUseCase.resetPassword(new ChangePasswordCommand(
                id,
                request.newPassword(),
                authentication.getName(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return ResponseEntity.noContent().build();
    }

    private static UserAdminDtos.UserDetailResponse toDetailResponse(UserDetailResult result) {
        return new UserAdminDtos.UserDetailResponse(
                result.id(),
                result.username(),
                result.fullName(),
                result.role(),
                result.active(),
                result.failedLoginAttempts(),
                result.lockedUntil(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
