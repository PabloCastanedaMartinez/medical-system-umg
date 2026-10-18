package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import java.util.List;

/**
 * Puerto de entrada para lectura de consultas médicas.
 */
public interface ConsultationReader {

    /**
     * Obtiene una consulta médica por su ID.
     * @param consultationId El ID de la consulta a obtener
     * @return La consulta encontrada
     * @throws ConsultationNotFoundException si la consulta no existe
     */
    Consultation getConsultationById(Long consultationId) throws ConsultationNotFoundException;

    /**
     * Obtiene todas las consultas médicas de un paciente.
     * @param patientId El ID del paciente cuyas consultas se obtienen
     * @return Lista de consultas del paciente ordenadas por fecha (más recientes primero)
     */
    List<Consultation> getConsultationsByPatientId(Long patientId);
}