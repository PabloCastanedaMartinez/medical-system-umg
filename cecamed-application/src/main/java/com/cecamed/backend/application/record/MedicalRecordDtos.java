package com.cecamed.backend.application.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTOs para el caso de uso de expediente médico.
 */
public class MedicalRecordDtos {

    /**
     * Solicitud para actualizar un expediente médico.
     * Contiene solo los campos editables.
     */
    public record Request(
            @Size(max = 2000) String allergies,
            @Size(max = 2000) String pathologicalHistory,
            @Size(max = 2000) String nonPathologicalHistory,
            @Size(max = 1000) String familyHistory,
            @Size(max = 2000) String surgicalHistory,
            @Size(max = 2000) String traumaticHistory,
            @Size(max = 1000) String currentMedications,
            @Size(max = 2000) String generalObservations,
            @Size(max = 1000) String gynecologicalObstetricHistory,
            @jakarta.validation.constraints.PositiveOrZero Integer waterGlassesPerDay,
            @jakarta.validation.constraints.PositiveOrZero Integer mealsPerDay,
            @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.PositiveOrZero Long version
    ) {}

    /**
     * Respuesta del expediente médico.
     */
    public record Response(
            Long patientId,
            String recordNumber,
            String allergies,
            String pathologicalHistory,
            String nonPathologicalHistory,
            String familyHistory,
            String surgicalHistory,
            String traumaticHistory,
            String currentMedications,
            String generalObservations,
            String gynecologicalObstetricHistory,
            Integer waterGlassesPerDay,
            Integer mealsPerDay,
            String createdAt,
            String updatedAt,
            Long version
    ) {}
}