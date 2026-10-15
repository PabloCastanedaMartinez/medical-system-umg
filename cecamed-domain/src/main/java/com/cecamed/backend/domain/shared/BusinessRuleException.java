package com.cecamed.backend.domain.shared;

/** Conflicto con una regla de negocio; el código es estable para los consumidores. */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
