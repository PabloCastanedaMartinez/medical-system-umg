package com.cecamed.backend.api.consultation;

import com.cecamed.backend.application.consultation.CreateConsultationUseCase;
import com.cecamed.backend.application.consultation.GetConsultationByIdUseCase;
import com.cecamed.backend.application.consultation.GetConsultationsByPatientIdUseCase;
import com.cecamed.backend.application.consultation.UpdateConsultationUseCase;
import com.cecamed.backend.application.consultation.ConsultationDtos;
import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para la gestión de consultas médicas.
 */
@RestController
@RequestMapping("/api/v1/patients/{patientId}/consultations")
@Tag(name = "Consultas Médicas", description = "Endpoints para gestión de consultas médicas de pacientes")
public class ConsultationController {

    private final GetConsultationByIdUseCase getConsultationByIdUseCase;
    private final GetConsultationsByPatientIdUseCase getConsultationsByPatientIdUseCase;
    private final CreateConsultationUseCase createConsultationUseCase;
    private final UpdateConsultationUseCase updateConsultationUseCase;

    public ConsultationController(GetConsultationByIdUseCase getConsultationByIdUseCase,
                                  GetConsultationsByPatientIdUseCase getConsultationsByPatientIdUseCase,
                                  CreateConsultationUseCase createConsultationUseCase,
                                  UpdateConsultationUseCase updateConsultationUseCase) {
        this.getConsultationByIdUseCase = getConsultationByIdUseCase;
        this.getConsultationsByPatientIdUseCase = getConsultationsByPatientIdUseCase;
        this.createConsultationUseCase = createConsultationUseCase;
        this.updateConsultationUseCase = updateConsultationUseCase;
    }

    @GetMapping
    @Operation(summary = "Obtener todas las consultas médicas de un paciente")
    public ResponseEntity<List<ConsultationDtos.Response>> getConsultations(
            @PathVariable Long patientId) {
        List<Consultation> consultations = getConsultationsByPatientIdUseCase.getConsultationsByPatientId(patientId);
        List<ConsultationDtos.Response> consultationResponses = consultations.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(consultationResponses);
    }

    @GetMapping("/{consultationId}")
    @Operation(summary = "Obtener una consulta médica específica por su ID")
    public ResponseEntity<ConsultationDtos.Response> getConsultation(
            @PathVariable Long patientId,
            @PathVariable Long consultationId) {
        Consultation consultation = getConsultationByIdUseCase.getConsultationById(consultationId);
        // Verify that the consultation belongs to the patient
        if (!consultation.getPatientId().equals(patientId)) {
            throw new ConsultationNotFoundException(consultationId);
        }
        return ResponseEntity.ok(toResponse(consultation));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva consulta médica para un paciente")
    public ResponseEntity<ConsultationDtos.Response> createConsultation(
            @PathVariable Long patientId,
            @Valid @RequestBody ConsultationDtos.Request request) {
        // Create consultation domain object from request
        Consultation consultation = new Consultation(
                patientId,
                /* consultationDate */ null, // Will be set by service
                request.consultationType(),
                request.doctorName(),
                request.reasonForVisit(),
                request.symptoms(),
                request.examinationFindings(),
                request.diagnosis(),
                request.treatmentPlan(),
                request.prescriptions(),
                request.followUpInstructions(),
                request.vitalSigns(),
                request.notes(),
                /* createdAt */ null, // Will be set by service
                /* updatedAt */ null  // Will be set by service
        );

        Consultation createdConsultation = createConsultationUseCase.createConsultation(consultation);
        return ResponseEntity.ok(toResponse(createdConsultation));
    }

    @PutMapping("/{consultationId}")
    @Operation(summary = "Actualizar una consulta médica existente")
    public ResponseEntity<ConsultationDtos.Response> updateConsultation(
            @PathVariable Long patientId,
            @PathVariable Long consultationId,
            @Valid @RequestBody ConsultationDtos.Request request) {
        // Get existing consultation first
        Consultation existingConsultation = getConsultationByIdUseCase.getConsultationById(consultationId);

        // Verify that the consultation belongs to the patient
        if (!existingConsultation.getPatientId().equals(patientId)) {
            throw new ConsultationNotFoundException(consultationId);
        }

        // Create updated consultation with existing values as base, overridden by request
        Consultation consultationToUpdate = new Consultation(
                existingConsultation.getConsultationId(),
                existingConsultation.getPatientId(),
                existingConsultation.getConsultationDate(),
                request.consultationType() != null ? request.consultationType() : existingConsultation.getConsultationType(),
                request.doctorName() != null ? request.doctorName() : existingConsultation.getDoctorName(),
                request.reasonForVisit() != null ? request.reasonForVisit() : existingConsultation.getReasonForVisit(),
                request.symptoms() != null ? request.symptoms() : existingConsultation.getSymptoms(),
                request.examinationFindings() != null ? request.examinationFindings() : existingConsultation.getExaminationFindings(),
                request.diagnosis() != null ? request.diagnosis() : existingConsultation.getDiagnosis(),
                request.treatmentPlan() != null ? request.treatmentPlan() : existingConsultation.getTreatmentPlan(),
                request.prescriptions() != null ? request.prescriptions() : existingConsultation.getPrescriptions(),
                request.followUpInstructions() != null ? request.followUpInstructions() : existingConsultation.getFollowUpInstructions(),
                request.vitalSigns() != null ? request.vitalSigns() : existingConsultation.getVitalSigns(),
                request.notes() != null ? request.notes() : existingConsultation.getNotes(),
                existingConsultation.getCreatedAt(), // Keep original createdAt
                null, // updatedAt will be set by service
                existingConsultation.getVersion() // Keep existing version
        );

        Consultation updatedConsultation = updateConsultationUseCase.updateConsultation(consultationId, consultationToUpdate);
        return ResponseEntity.ok(toResponse(updatedConsultation));
    }

    private ConsultationDtos.Response toResponse(Consultation consultation) {
        return new ConsultationDtos.Response(
                consultation.getConsultationId(),
                consultation.getPatientId(),
                consultation.getConsultationType(),
                consultation.getDoctorName(),
                consultation.getReasonForVisit(),
                consultation.getSymptoms(),
                consultation.getExaminationFindings(),
                consultation.getDiagnosis(),
                consultation.getTreatmentPlan(),
                consultation.getPrescriptions(),
                consultation.getFollowUpInstructions(),
                consultation.getVitalSigns(),
                consultation.getNotes(),
                consultation.getCreatedAt().toString(),
                consultation.getUpdatedAt().toString()
        );
    }
}