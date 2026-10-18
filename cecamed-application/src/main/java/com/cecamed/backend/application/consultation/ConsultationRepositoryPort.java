package com.cecamed.backend.application.consultation;

import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de repositorio para el caso de uso de consulta médica.
 * Define las operaciones que el caso de uso necesita del repositorio.
 */
public interface ConsultationRepositoryPort {

    /**
     * Guarda una consulta médica.
     * @param consulta La consulta a guardar
     * @return La consulta guardada con su ID y versión asignados
     */
    Consultation save(Consultation consulta);

    /**
     * Busca una consulta médica por su ID.
     * @param consultaId El ID de la consulta a buscar
     * @return La consulta encontrada, o vacía si no existe
     */
    Optional<Consultation> findById(Long consultaId);

    /**
     * Busca todas las consultas médicas de un paciente.
     * @param patientId El ID del paciente cuyas consultas se buscan
     * @return Lista de consultas del paciente ordenadas por fecha (más recientes primero)
     */
    List<Consultation> findByPatientId(Long patientId);

    /**
     * Elimina una consulta médica por su ID.
     * @param consultaId El ID de la consulta a eliminar
     */
    void deleteById(Long consultaId);
}