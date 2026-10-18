package com.cecamed.backend.application.record;

import com.cecamed.backend.domain.record.MedicalRecord;

/**
 * Caso de uso para actualizar el expediente médico de un paciente.
 */
public interface UpdateMedicalRecordUseCase {

    MedicalRecord updateMedicalRecord(Long patientId, MedicalRecord medicalRecord);
}