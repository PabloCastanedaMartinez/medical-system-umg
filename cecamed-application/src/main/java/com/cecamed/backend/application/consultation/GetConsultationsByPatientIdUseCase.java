package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;
import java.util.List;

/**
 * Caso de uso para obtener todas las consultas médicas de un paciente.
 */
public interface GetConsultationsByPatientIdUseCase {

    /**
     * Obtiene todas las consultas médicas de un paciente.
     * @param patientId El ID del paciente cuyas consultas se obtienen
     * @return Lista de consultas del paciente ordenadas por fecha (más recientes primero)
     */
    List<Consultation> getConsultationsByPatientId(Long patientId);
}