package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;

/**
 * Puerto de entrada para escritura de consultas médicas.
 */
public interface ConsultationWriter {

    /**
     * Crea una nueva consulta médica.
     * @param consulta La consulta a crear
     * @return La consulta creada con su ID y versión asignados
     */
    Consultation createConsultation(Consultation consulta);

    /**
     * Actualiza una consulta médica existente.
     * @param consultaId El ID de la consulta a actualizar
     * @param consulta La consulta con los valores actualizados
     * @return La consulta actualizada
     */
    Consultation updateConsultation(Long consultaId, Consultation consulta);
}