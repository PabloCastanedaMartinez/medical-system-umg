package com.cecamed.backend.api.identity;

import com.cecamed.backend.api.identity.dto.AuthDtos;
import com.cecamed.backend.application.identity.model.CurrentUserResult;
import com.cecamed.backend.application.identity.model.LoginCommand;
import com.cecamed.backend.application.identity.model.LoginResult;
import com.cecamed.backend.application.identity.model.LogoutCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenCommand;
import com.cecamed.backend.application.identity.model.RefreshTokenResult;
import com.cecamed.backend.application.identity.port.in.GetCurrentUserUseCase;
import com.cecamed.backend.application.identity.port.in.LoginUseCase;
import com.cecamed.backend.application.identity.port.in.LogoutUseCase;
import com.cecamed.backend.application.identity.port.in.RefreshTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticación", description = "Endpoints para inicio, refresco, cierre de sesión y consulta del usuario actual.")
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetCurrentUserUseCase getCurrentUserUseCase;

    public AuthController(
            LoginUseCase loginUseCase,
            RefreshTokenUseCase refreshTokenUseCase,
            LogoutUseCase logoutUseCase,
            GetCurrentUserUseCase getCurrentUserUseCase) {
        this.loginUseCase = Objects.requireNonNull(loginUseCase);
        this.refreshTokenUseCase = Objects.requireNonNull(refreshTokenUseCase);
        this.logoutUseCase = Objects.requireNonNull(logoutUseCase);
        this.getCurrentUserUseCase = Objects.requireNonNull(getCurrentUserUseCase);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener tokens de acceso y actualización")
    public AuthDtos.LoginResponse login(
            @Valid @RequestBody AuthDtos.LoginRequest request,
            HttpServletRequest servletRequest) {
        LoginResult result = loginUseCase.login(new LoginCommand(
                request.username(),
                request.password(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return new AuthDtos.LoginResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresInSeconds(),
                result.refreshToken(),
                result.username(),
                result.fullName(),
                result.role(),
                result.roleDisplayName(),
                result.loginTime()
        );
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotar el refresh token y obtener un nuevo access token")
    public AuthDtos.RefreshTokenResponse refresh(
            @Valid @RequestBody AuthDtos.RefreshTokenRequest request,
            HttpServletRequest servletRequest) {
        RefreshTokenResult result = refreshTokenUseCase.refresh(new RefreshTokenCommand(
                request.refreshToken(),
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return new AuthDtos.RefreshTokenResponse(
                result.accessToken(),
                result.tokenType(),
                result.expiresInSeconds(),
                result.refreshToken()
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar el refresh token")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) AuthDtos.LogoutRequest request,
            Authentication authentication,
            HttpServletRequest servletRequest) {
        String username = authentication != null ? authentication.getName() : null;
        String refreshToken = request != null ? request.refreshToken() : null;

        logoutUseCase.logout(new LogoutCommand(
                refreshToken,
                username,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        ));

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener información del usuario actualmente autenticado")
    public AuthDtos.CurrentUserResponse me(Authentication authentication) {
        CurrentUserResult result = getCurrentUserUseCase.getCurrentUser(authentication.getName());

        return new AuthDtos.CurrentUserResponse(
                result.username(),
                result.fullName(),
                result.role(),
                result.roleDisplayName(),
                result.loginTime()
        );
    }
}
