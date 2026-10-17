package com.cecamed.backend.infrastructure.config;

import com.cecamed.backend.application.identity.port.out.BruteForcePolicyPort;
import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.PasswordPolicyPort;
import com.cecamed.backend.application.identity.port.out.RefreshTokenRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.identity.port.out.TokenIssuerPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.application.identity.service.AuthenticationService;
import com.cecamed.backend.application.identity.service.UserAdminService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class IdentityServiceConfiguration {

    @Bean
    public AuthenticationService authenticationService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            SecurityAuditRepositoryPort auditRepository,
            TokenIssuerPort tokenIssuer,
            PasswordEncoderPort passwordEncoder,
            BruteForcePolicyPort bruteForcePolicy,
            Clock clock) {
        return new AuthenticationService(
                userRepository,
                refreshTokenRepository,
                auditRepository,
                tokenIssuer,
                passwordEncoder,
                bruteForcePolicy,
                clock
        );
    }

    @Bean
    public UserAdminService userAdminService(
            UserRepositoryPort userRepository,
            RefreshTokenRepositoryPort refreshTokenRepository,
            SecurityAuditRepositoryPort auditRepository,
            PasswordEncoderPort passwordEncoder,
            PasswordPolicyPort passwordPolicy,
            Clock clock) {
        return new UserAdminService(
                userRepository,
                refreshTokenRepository,
                auditRepository,
                passwordEncoder,
                passwordPolicy,
                clock
        );
    }
}
