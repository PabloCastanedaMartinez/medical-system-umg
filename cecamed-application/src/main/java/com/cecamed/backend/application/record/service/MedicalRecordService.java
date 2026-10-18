package com.cecamed.backend.application.record.service;

import com.cecamed.backend.application.record.GetMedicalRecordUseCase;
import com.cecamed.backend.application.record.MedicalRecordDtos;
import com.cecamed.backend.application.record.UpdateMedicalRecordUseCase;
import com.cecamed.backend.application.record.MedicalRecordRepositoryPort;
import com.cecamed.backend.domain.record.MedicalRecord;
import com.cecamed.backend.domain.record.MedicalRecordNotFoundException;

/**
 * Implementación del servicio de expediente médico.
 */
public class MedicalRecordService implements GetMedicalRecordUseCase, UpdateMedicalRecordUseCase {

    private final MedicalRecordRepositoryPort medicalRecordRepository;

    private final java.time.Clock clock;

    public MedicalRecordService(MedicalRecordRepositoryPort medicalRecordRepository, java.time.Clock clock) {
        this.clock = clock;
        this.medicalRecordRepository = medicalRecordRepository;
    }

    @Override
    public MedicalRecord getMedicalRecord(Long patientId) {
        return medicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(patientId));
    }

    @Override
    public MedicalRecord updateMedicalRecord(Long patientId, MedicalRecord medicalRecord) {
        // Verify that the medical record belongs to the patient
        MedicalRecord existing = medicalRecordRepository.findByPatientId(patientId)
                .orElseThrow(() -> new MedicalRecordNotFoundException(patientId));

        if (medicalRecord.getVersion() == null || !medicalRecord.getVersion().equals(existing.getVersion())) {
            throw new com.cecamed.backend.domain.patient.StaleVersionException("El expediente cambió. Recargue antes de editar.");
        }
        // Update the fields (keeping the same patientId and version for optimistic locking)
        MedicalRecord updated = new MedicalRecord(
                existing.getPatientId(),
                existing.getRecordNumber(),
                medicalRecord.getAllergies(),
                medicalRecord.getPathologicalHistory(),
                medicalRecord.getNonPathologicalHistory(),
                medicalRecord.getFamilyHistory(),
                medicalRecord.getSurgicalHistory(),
                medicalRecord.getTraumaticHistory(),
                medicalRecord.getCurrentMedications(),
                medicalRecord.getGeneralObservations(),
                medicalRecord.getGynecologicalObstetricHistory(),
                medicalRecord.getWaterGlassesPerDay(),
                medicalRecord.getMealsPerDay(),
                existing.getCreatedAt(), // Keep original createdAt
                clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                existing.getVersion() // Keep existing version - will be incremented by JPA
        );

        return medicalRecordRepository.save(updated);
    }
}