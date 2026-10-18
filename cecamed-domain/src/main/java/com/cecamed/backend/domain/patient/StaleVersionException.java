package com.cecamed.backend.domain.patient;

import com.cecamed.backend.domain.shared.BusinessRuleException;

/**
 * Excepción lanzada cuando hay un conflicto de versión al actualizar un paciente (bloqueo optimista).
 */
public class StaleVersionException extends BusinessRuleException {

    public StaleVersionException(String message) {
        super("PATIENT_VERSION_CONFLICT", message);
    }
}