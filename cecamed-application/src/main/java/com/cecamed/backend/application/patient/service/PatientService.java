package com.cecamed.backend.application.patient.service;

import com.cecamed.backend.application.patient.model.PatientRequest;
import com.cecamed.backend.application.patient.model.PatientResponse;
import com.cecamed.backend.application.patient.port.in.PatientManagementUseCase;
import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.domain.patient.Patient;
import com.cecamed.backend.domain.patient.PatientAlreadyExistsException;
import com.cecamed.backend.domain.patient.PatientInactiveException;
import com.cecamed.backend.domain.shared.BusinessRuleException;
import com.cecamed.backend.domain.identity.AuditEventType;
import com.cecamed.backend.domain.identity.SecurityAuditEvent;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Servicio de aplicación para la gestión de pacientes.
 * Implementa los casos de uso definidos en PatientManagementUseCase.
 */
public class PatientService implements PatientManagementUseCase {

    private static final int MAX_PAGE_SIZE = 100; // Límite máximo de tamaño de página

    private final PatientRepositoryPort patientRepository;
    private final SecurityAuditRepositoryPort auditRepository;
    private final Clock clock;

    public PatientService(PatientRepositoryPort patientRepository,
                          SecurityAuditRepositoryPort auditRepository,
                          Clock clock) {
        this.patientRepository = Objects.requireNonNull(patientRepository);
        this.auditRepository = Objects.requireNonNull(auditRepository);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public PatientResponse createPatient(PatientRequest request, String performedBy, String ipAddress, String userAgent) {
        String identificationNumber = request.getIdentificationNumber();

        // La identificación es opcional; el dominio la normaliza.
        Patient patient = Patient.createNew(
                clock,
                identificationNumber,
                request.getFirstName(),
                request.getLastName(),
                request.getBirthDate(),
                request.getGender(),
                request.getBloodType(),
                request.getPhone(),
                request.getEmail(),
                request.getAddress(),
                request.getEmergencyContactName(),
                request.getEmergencyContactPhone(),
                request.getEmergencyContactRelationship(),
                request.getNotes()
        );

        // Guardar paciente
        Patient savedPatient = patientRepository.save(patient);

        // Auditoría de creación
        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.PATIENT_CREATED,
                performedBy,
                ipAddress,
                userAgent,
                clock.instant(),
                true,
                "Paciente creado con ID: " + savedPatient.getId()
        ));

        // Mapear a DTO de respuesta
        return toPatientResponse(savedPatient);
    }

    @Override
    public PatientResponse getPatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado con ID: " + id));

        return toPatientResponse(patient);
    }

    @Override
    public com.cecamed.backend.application.identity.model.PagedResult<PatientResponse> searchPatients(int page, int size, String search, Boolean active) {
        // Validar y ajustar tamaño de página
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        if (size <= 0) {
            size = MAX_PAGE_SIZE;
        }
        if (page < 0) {
            page = 0;
        }

        // Normalizar término de búsqueda (trim)
        String trimmedSearch = (search == null) ? null : search.trim();

        // Buscar pacientes en el repositorio
        com.cecamed.backend.application.identity.model.PagedResult<Patient> pagedPatients =
                patientRepository.search(page, size, trimmedSearch, active);

        // Mapear cada paciente a DTO de respuesta
        var patientResponses = pagedPatients.content().stream()
                .map(this::toPatientResponse)
                .toList();

        // Devolver resultado paginado con los DTOs
        return new com.cecamed.backend.application.identity.model.PagedResult<>(
                patientResponses,
                pagedPatients.page(),
                pagedPatients.size(),
                pagedPatients.totalElements(),
                pagedPatients.totalPages()
        );
    }

    @Override
    public PatientResponse updatePatient(Long id, PatientRequest request, String performedBy, String ipAddress, String userAgent) {
        // Buscar paciente existente
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado con ID: " + id));

        if (request.getVersion() == null || !request.getVersion().equals(patient.getVersion())) {
            throw new com.cecamed.backend.domain.patient.StaleVersionException("El paciente cambió. Recargue antes de editar.");
        }
        // Actualizar información (lanza PatientInactiveException si está inactivo)
        patient.updateInformation(
                clock,
                request.getFirstName(),
                request.getLastName(),
                request.getBirthDate(),
                request.getGender(),
                request.getBloodType(),
                request.getPhone(),
                request.getEmail(),
                request.getAddress(),
                request.getEmergencyContactName(),
                request.getEmergencyContactPhone(),
                request.getEmergencyContactRelationship(),
                request.getNotes()
        );

        // Guardar cambios
        Patient savedPatient = patientRepository.save(patient);

        // Auditoría de actualización
        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.PATIENT_UPDATED,
                performedBy,
                ipAddress,
                userAgent,
                clock.instant(),
                true,
                "Paciente actualizado con ID: " + savedPatient.getId()
        ));

        // Mapear a DTO de respuesta
        return toPatientResponse(savedPatient);
    }

    @Override
    public void activatePatient(Long id, Long version, String performedBy, String ipAddress, String userAgent) {
        // Buscar paciente existente
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado con ID: " + id));

        if (version == null || !version.equals(patient.getVersion())) {
            throw new com.cecamed.backend.domain.patient.StaleVersionException("El paciente cambió. Recargue antes de cambiar su estado.");
        }
        // Verificar si ya está activo
        if (patient.isActive()) {
            throw new BusinessRuleException("PATIENT_ALREADY_ACTIVE",
                    "El paciente con ID " + id + " ya está activo");
        }

        // Activar paciente
        patient.activate(clock);

        // Guardar cambios
        patientRepository.save(patient);

        // Auditoría de activación
        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.PATIENT_ACTIVATED,
                performedBy,
                ipAddress,
                userAgent,
                clock.instant(),
                true,
                "Paciente activado con ID: " + id
        ));
    }

    @Override
    public void deactivatePatient(Long id, Long version, String performedBy, String ipAddress, String userAgent) {
        // Buscar paciente existente
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("PATIENT_NOT_FOUND",
                        "Paciente no encontrado con ID: " + id));

        if (version == null || !version.equals(patient.getVersion())) {
            throw new com.cecamed.backend.domain.patient.StaleVersionException("El paciente cambió. Recargue antes de cambiar su estado.");
        }
        // Verificar si ya está inactivo
        if (!patient.isActive()) {
            throw new BusinessRuleException("PATIENT_ALREADY_INACTIVE",
                    "El paciente con ID " + id + " ya está inactivo");
        }

        // Desactivar paciente
        patient.deactivate(clock);

        // Guardar cambios
        patientRepository.save(patient);

        // Auditoría de desactivación
        auditRepository.save(SecurityAuditEvent.of(
                AuditEventType.PATIENT_DEACTIVATED,
                performedBy,
                ipAddress,
                userAgent,
                clock.instant(),
                true,
                "Paciente desactivado con ID: " + id
        ));
    }

    // --- Métodos auxiliares privados ---

    private PatientResponse toPatientResponse(Patient patient) {
        // Calcular edad a partir de la fecha de nacimiento
        Integer age = null;
        if (patient.getBirthDate() != null) {
            age = patient.getBirthDate().until(java.time.LocalDate.now(clock.withZone(java.time.ZoneId.of("America/Guatemala")))).getYears();
        }

        String createdAt = patient.getCreatedAt().toString();
        String updatedAt = patient.getUpdatedAt().toString();

        var response = new PatientResponse(
                patient.getId(),
                patient.getIdentificationNumber(),
                patient.getFirstName(),
                patient.getLastName(),
                patient.getFullName(),
                patient.getBirthDate() != null ? patient.getBirthDate().toString() : null,
                age,
                patient.getGender(),
                patient.getBloodType(),
                patient.getPhone(),
                patient.getEmail(),
                patient.getAddress(),
                patient.getEmergencyContactName(),
                patient.getEmergencyContactPhone(),
                patient.getEmergencyContactRelationship(),
                patient.getNotes(),
                patient.isActive(),
                patient.getMedicalRecordNumber(),
                createdAt,
                updatedAt
        );
        response.setVersion(patient.getVersion());
        return response;
    }
}