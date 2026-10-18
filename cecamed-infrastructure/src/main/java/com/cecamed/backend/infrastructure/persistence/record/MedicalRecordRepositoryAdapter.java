package com.cecamed.backend.infrastructure.persistence.record;

import com.cecamed.backend.application.record.MedicalRecordRepositoryPort;
import com.cecamed.backend.domain.record.MedicalRecord;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Adaptador del repositorio de expedientes médicos.
 */
@Component
@Transactional
public class MedicalRecordRepositoryAdapter implements MedicalRecordRepositoryPort {

    private final MedicalRecordSpringDataRepository repository;

    public MedicalRecordRepositoryAdapter(MedicalRecordSpringDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<MedicalRecord> findByPatientId(Long patientId) {
        return repository.findById(patientId)
                .map(MedicalRecordJpaEntity::toDomain);
    }

    @Override
    public MedicalRecord save(MedicalRecord medicalRecord) {
        MedicalRecordJpaEntity entity = MedicalRecordJpaEntity.fromDomain(medicalRecord);
        return repository.saveAndFlush(entity).toDomain();
    }
}