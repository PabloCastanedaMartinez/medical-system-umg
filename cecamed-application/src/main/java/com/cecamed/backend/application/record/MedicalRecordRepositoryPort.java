package com.cecamed.backend.application.record;

import com.cecamed.backend.domain.record.MedicalRecord;
import java.util.Optional;

/**
 * Puerto de salida para persistencia de expedientes médicos.
 */
public interface MedicalRecordRepositoryPort {

    Optional<MedicalRecord> findByPatientId(Long patientId);

    MedicalRecord save(MedicalRecord medicalRecord);
}