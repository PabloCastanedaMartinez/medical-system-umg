package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;

/**
 * Caso de uso para crear una nueva consulta médica.
 */
public interface CreateConsultationUseCase {

    /**
     * Crea una nueva consulta médica.
     * @param consulta La consulta a crear
     * @return La consulta creada con su ID y versión asignados
     */
    Consultation createConsultation(Consultation consulta);
}