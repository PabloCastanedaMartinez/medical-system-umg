package com.cecamed.backend.application.identity.port.out;

public interface PasswordPolicyPort {
    void validatePassword(String password);
}
