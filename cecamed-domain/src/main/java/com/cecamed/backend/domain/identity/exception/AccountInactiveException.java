package com.cecamed.backend.domain.identity.exception;

public class AccountInactiveException extends RuntimeException {
    public AccountInactiveException(String message) {
        super(message);
    }
}
