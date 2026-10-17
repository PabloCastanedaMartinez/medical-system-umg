package com.cecamed.backend.domain.patient;

import com.cecamed.backend.domain.shared.BusinessRuleException;

/**
 * Excepción lanzada cuando se intenta realizar una operación en un paciente inactivo.
 */
public class PatientInactiveException extends BusinessRuleException {

    public PatientInactiveException(String message) {
        super("PATIENT_INACTIVE", message);
    }
}