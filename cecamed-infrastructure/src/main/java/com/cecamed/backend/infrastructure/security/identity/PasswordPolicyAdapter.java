package com.cecamed.backend.infrastructure.security.identity;

import com.cecamed.backend.application.identity.port.out.PasswordPolicyPort;
import com.cecamed.backend.domain.identity.exception.PasswordPolicyException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyAdapter implements PasswordPolicyPort {

    private static final int MIN_LENGTH = 12;

    @Override
    public void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new PasswordPolicyException("La contraseña no puede estar vacía.");
        }
        if (password.length() < MIN_LENGTH) {
            throw new PasswordPolicyException("La contraseña debe contener al menos " + MIN_LENGTH + " caracteres.");
        }
    }
}
