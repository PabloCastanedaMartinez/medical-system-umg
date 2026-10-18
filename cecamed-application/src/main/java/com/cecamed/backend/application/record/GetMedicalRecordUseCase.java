package com.cecamed.backend.application.record;

import com.cecamed.backend.domain.record.MedicalRecord;

/**
 * Caso de uso para obtener el expediente médico de un paciente.
 */
public interface GetMedicalRecordUseCase {

    MedicalRecord getMedicalRecord(Long patientId);
}