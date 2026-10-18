package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;

/**
 * Caso de uso para actualizar una consulta médica existente.
 */
public interface UpdateConsultationUseCase {

    /**
     * Actualiza una consulta médica existente.
     * @param consultationId El ID de la consulta a actualizar
     * @param consulta La consulta con los valores actualizados
     * @return La consulta actualizada
     */
    Consultation updateConsultation(Long consultationId, Consultation consulta);
}