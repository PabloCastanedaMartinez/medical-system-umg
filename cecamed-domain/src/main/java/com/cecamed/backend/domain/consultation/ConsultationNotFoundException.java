package com.cecamed.backend.domain.consultation;

import java.time.Instant;

/**
 * Excepción lanzada cuando no se encuentra la consulta médica solicitada.
 */
public class ConsultationNotFoundException extends RuntimeException {

    public ConsultationNotFoundException(Long consultationId) {
        super("Consultation not found for ID: " + consultationId);
    }

    public ConsultationNotFoundException(Long patientId, Instant consultationDate) {
        super("Consultation not found for patient ID: " + patientId +
              " at date: " + consultationDate);
    }
}