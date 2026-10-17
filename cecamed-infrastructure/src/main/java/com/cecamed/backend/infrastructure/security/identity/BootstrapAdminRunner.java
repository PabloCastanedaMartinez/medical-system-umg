package com.cecamed.backend.infrastructure.security.identity;

import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdminRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminRunner.class);

    private final UserRepositoryPort userRepository;
    private final PasswordEncoderPort passwordEncoder;
    private final Clock clock;
    private final String bootstrapUsername;
    private final String bootstrapPassword;
    private final String bootstrapFullName;

    public BootstrapAdminRunner(
            UserRepositoryPort userRepository,
            PasswordEncoderPort passwordEncoder,
            Clock clock,
            @Value("${cecamed.security.bootstrap-admin.username:}") String bootstrapUsername,
            @Value("${cecamed.security.bootstrap-admin.password:}") String bootstrapPassword,
            @Value("${cecamed.security.bootstrap-admin.full-name:Administrador Inicial}") String bootstrapFullName) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.clock = Objects.requireNonNull(clock);
        this.bootstrapUsername = bootstrapUsername;
        this.bootstrapPassword = bootstrapPassword;
        this.bootstrapFullName = bootstrapFullName;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (bootstrapUsername.isBlank() && bootstrapPassword.isBlank()) {
            log.info("No se especificaron variables para bootstrap de administrador; omitiendo.");
            return;
        }

        if (bootstrapUsername.isBlank() || bootstrapPassword.length() < 12) {
            throw new IllegalStateException("Para inicializar el administrador, configure username y un password de al menos 12 caracteres.");
        }

        String normalizedUsername = bootstrapUsername.trim().toLowerCase();
        if (!userRepository.existsByUsername(normalizedUsername)) {
            Instant now = clock.instant();
            UserAccount admin = UserAccount.createNew(
                    normalizedUsername,
                    passwordEncoder.encode(bootstrapPassword),
                    bootstrapFullName.trim(),
                    Role.ADMIN,
                    now
            );
            userRepository.save(admin);
            log.info("Usuario administrador inicial '{}' creado exitosamente.", normalizedUsername);
        } else {
            log.info("El usuario administrador inicial '{}' ya existe.", normalizedUsername);
        }
    }
}
