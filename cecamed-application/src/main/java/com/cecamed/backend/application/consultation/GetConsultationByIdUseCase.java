package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;

/**
 * Caso de uso para obtener una consulta médica por su ID.
 */
public interface GetConsultationByIdUseCase {

    /**
     * Obtiene una consulta médica por su ID.
     * @param consultationId El ID de la consulta a obtener
     * @return La consulta encontrada
     * @throws ConsultationNotFoundException si la consulta no existe
     */
    Consultation getConsultationById(Long consultationId) throws ConsultationNotFoundException;
}