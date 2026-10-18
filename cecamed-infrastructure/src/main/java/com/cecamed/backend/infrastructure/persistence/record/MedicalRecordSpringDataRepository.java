package com.cecamed.backend.infrastructure.persistence.record;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repositorio Spring Data para expedientes médicos.
 */
@Repository
public interface MedicalRecordSpringDataRepository extends JpaRepository<MedicalRecordJpaEntity, Long> {
}