package com.cecamed.backend.application.consultation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs para el caso de uso de consulta médica.
 */
public class ConsultationDtos {

    /**
     * Solicitud para crear o actualizar una consulta médica.
     * Contiene solo los campos editables.
     */
    public record Request(
            @NotBlank @Size(max = 100) String consultationType,
            @NotBlank @Size(max = 200) String doctorName,
            @NotBlank @Size(max = 2000) String reasonForVisit,
            @Size(max = 2000) String symptoms,
            @Size(max = 2000) String examinationFindings,
            @Size(max = 1000) String diagnosis,
            @Size(max = 2000) String treatmentPlan,
            @Size(max = 2000) String prescriptions,
            @Size(max = 2000) String followUpInstructions,
            @Size(max = 2000) String vitalSigns,
            @Size(max = 2000) String notes,
            @Size(max = 20) String icd10Code,
            @jakarta.validation.constraints.PositiveOrZero Long version
    ) {}

    /**
     * Respuesta de la consulta médica.
     */
    public record Response(
            Long consultationId,
            Long patientId,
            String consultationType,
            String doctorName,
            String reasonForVisit,
            String symptoms,
            String examinationFindings,
            String diagnosis,
            String treatmentPlan,
            String prescriptions,
            String followUpInstructions,
            String vitalSigns,
            String notes,
            String createdAt,
            String updatedAt,
            String consultationDate,
            String icd10Code,
            Long version
    ) {}
}