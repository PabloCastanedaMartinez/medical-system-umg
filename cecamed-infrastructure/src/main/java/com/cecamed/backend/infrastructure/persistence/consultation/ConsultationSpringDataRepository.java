package com.cecamed.backend.infrastructure.persistence.consultation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

/**
 * Repositorio Spring Data JPA para consultas médicas.
 */
public interface ConsultationSpringDataRepository extends JpaRepository<ConsultationJpaEntity, Long> {

    /**
     * Encuentra una consulta por su ID.
     * @param consultationId El ID de la consulta
     * @return La consulta encontrada
     */
    ConsultationJpaEntity findByConsultationId(Long consultationId);

    /**
     * Encuentra todas las consultas de un paciente ordenadas por fecha (más recientes primero).
     * @param patientId El ID del paciente
     * @return Lista de consultas del paciente
     */
    @Query("SELECT c FROM ConsultationJpaEntity c WHERE c.patientId = :patientId ORDER BY c.consultationDate DESC")
    List<ConsultationJpaEntity> findByPatientId(@Param("patientId") Long patientId);
}