package com.cecamed.backend.api.record;

import com.cecamed.backend.application.record.GetMedicalRecordUseCase;
import com.cecamed.backend.application.record.MedicalRecordDtos;
import com.cecamed.backend.application.record.UpdateMedicalRecordUseCase;
import com.cecamed.backend.domain.record.MedicalRecord;
import com.cecamed.backend.domain.record.MedicalRecordNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/medical-record")
@Tag(name = "Expediente Médico", description = "Endpoints para gestión del expediente clínico del paciente")
public class MedicalRecordController {

    private final GetMedicalRecordUseCase getMedicalRecordUseCase;
    private final UpdateMedicalRecordUseCase updateMedicalRecordUseCase;

    public MedicalRecordController(GetMedicalRecordUseCase getMedicalRecordUseCase,
                                   UpdateMedicalRecordUseCase updateMedicalRecordUseCase) {
        this.getMedicalRecordUseCase = getMedicalRecordUseCase;
        this.updateMedicalRecordUseCase = updateMedicalRecordUseCase;
    }

    @GetMapping
    @Operation(summary = "Obtener el expediente médico del paciente")
    public ResponseEntity<MedicalRecordDtos.Response> getMedicalRecord(
            @PathVariable Long patientId) {
        var medicalRecord = getMedicalRecordUseCase.getMedicalRecord(patientId);
        return ResponseEntity.ok(toResponse(medicalRecord));
    }

    @PutMapping
    @Operation(summary = "Actualizar el expediente médico del paciente")
    public ResponseEntity<MedicalRecordDtos.Response> updateMedicalRecord(
            @PathVariable Long patientId,
            @Valid @RequestBody MedicalRecordDtos.Request request) {
        // Get existing record first
        MedicalRecord existingRecord = getMedicalRecordUseCase.getMedicalRecord(patientId);
        
        // Create update record with existing values as base, overridden by request
        MedicalRecord medicalRecordToUpdate = new MedicalRecord(
                existingRecord.getPatientId(),
                existingRecord.getRecordNumber(),
                request.allergies() != null ? request.allergies() : existingRecord.getAllergies(),
                request.pathologicalHistory() != null ? request.pathologicalHistory() : existingRecord.getPathologicalHistory(),
                request.nonPathologicalHistory() != null ? request.nonPathologicalHistory() : existingRecord.getNonPathologicalHistory(),
                request.familyHistory() != null ? request.familyHistory() : existingRecord.getFamilyHistory(),
                request.surgicalHistory() != null ? request.surgicalHistory() : existingRecord.getSurgicalHistory(),
                request.traumaticHistory() != null ? request.traumaticHistory() : existingRecord.getTraumaticHistory(),
                request.currentMedications() != null ? request.currentMedications() : existingRecord.getCurrentMedications(),
                request.generalObservations() != null ? request.generalObservations() : existingRecord.getGeneralObservations(),
                request.gynecologicalObstetricHistory() != null ? request.gynecologicalObstetricHistory() : existingRecord.getGynecologicalObstetricHistory(),
                request.waterGlassesPerDay() != null ? request.waterGlassesPerDay() : existingRecord.getWaterGlassesPerDay(),
                request.mealsPerDay() != null ? request.mealsPerDay() : existingRecord.getMealsPerDay(),
                existingRecord.getCreatedAt(), // Keep original createdAt
                null // updatedAt will be set by service
        );

        var updatedMedicalRecord = updateMedicalRecordUseCase.updateMedicalRecord(patientId, medicalRecordToUpdate);
        return ResponseEntity.ok(toResponse(updatedMedicalRecord));
    }

    private MedicalRecordDtos.Response toResponse(com.cecamed.backend.domain.record.MedicalRecord record) {
        return new MedicalRecordDtos.Response(
                record.getPatientId(),
                record.getRecordNumber(),
                record.getAllergies(),
                record.getPathologicalHistory(),
                record.getNonPathologicalHistory(),
                record.getFamilyHistory(),
                record.getSurgicalHistory(),
                record.getTraumaticHistory(),
                record.getCurrentMedications(),
                record.getGeneralObservations(),
                record.getGynecologicalObstetricHistory(),
                record.getWaterGlassesPerDay(),
                record.getMealsPerDay(),
                record.getCreatedAt().toString(),
                record.getUpdatedAt().toString()
        );
    }
}