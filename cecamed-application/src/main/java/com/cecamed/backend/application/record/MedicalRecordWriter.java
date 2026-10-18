package com.cecamed.backend.application.record;

import com.cecamed.backend.domain.record.MedicalRecord;

/**
 * Puerto de entrada para escritura de expedientes médicos.
 */
public interface MedicalRecordWriter {

    MedicalRecord updateMedicalRecord(Long patientId, MedicalRecord medicalRecord);
}