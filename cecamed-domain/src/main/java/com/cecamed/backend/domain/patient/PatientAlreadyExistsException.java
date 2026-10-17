package com.cecamed.backend.domain.patient;

import com.cecamed.backend.domain.shared.BusinessRuleException;

/**
 * Excepción lanzada cuando se intenta crear un paciente con un número de identificación que ya existe.
 */
public class PatientAlreadyExistsException extends BusinessRuleException {

    public PatientAlreadyExistsException(String message) {
        super("PATIENT_ALREADY_EXISTS", message);
    }
}