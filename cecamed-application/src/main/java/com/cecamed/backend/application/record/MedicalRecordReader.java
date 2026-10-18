package com.cecamed.backend.application.record;

import com.cecamed.backend.domain.record.MedicalRecord;

/**
 * Puerto de entrada para lectura de expedientes médicos.
 */
public interface MedicalRecordReader {

    MedicalRecord getMedicalRecord(Long patientId);
}