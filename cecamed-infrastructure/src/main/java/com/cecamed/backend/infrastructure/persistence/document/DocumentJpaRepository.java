package com.cecamed.backend.infrastructure.persistence.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DocumentJpaRepository extends JpaRepository<DocumentJpaEntity, Long> {
    List<DocumentJpaEntity> findByPatientIdAndActiveTrueOrderByCreatedAtDescIdDesc(long patientId);
}
