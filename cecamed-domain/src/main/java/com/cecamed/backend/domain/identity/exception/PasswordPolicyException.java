package com.cecamed.backend.domain.identity.exception;

public class PasswordPolicyException extends RuntimeException {
    public PasswordPolicyException(String message) {
        super(message);
    }
}
