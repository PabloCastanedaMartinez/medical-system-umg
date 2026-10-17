package com.cecamed.backend.infrastructure.persistence.patient;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data para la entidad PatientJpaEntity.
 */
@Repository
public interface PatientSpringDataRepository extends JpaRepository<PatientJpaEntity, Long> {

    boolean existsByIdentificationNumber(String identificationNumber);

}