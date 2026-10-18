package com.cecamed.backend.api.patient;

import com.cecamed.backend.application.patient.model.PatientRequest;
import com.cecamed.backend.application.patient.model.PatientResponse;
import com.cecamed.backend.application.patient.port.in.PatientManagementUseCase;
import com.cecamed.backend.application.identity.model.PagedResult;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Controlador REST para la gestión de pacientes.
 * Maneja los endpoints relacionados con pacientes.
 */
@RestController
@RequestMapping("/api/v1/patients")
public class PatientController {

    private final PatientManagementUseCase patientUseCase;

    public PatientController(PatientManagementUseCase patientUseCase) {
        this.patientUseCase = patientUseCase;
    }

    /**
     * Lista pacientes con búsqueda y filtros.
     *
     * @param search término de búsqueda (nombre, apellido, identificación, etc.)
     * @param status filtro de estado: ACTIVE, INACTIVE, ALL
     * @return lista de pacientes
     */
    @GetMapping
    public ResponseEntity<PagedResult<PatientResponse>> listPatients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        if (status != null && !java.util.Set.of("ACTIVE", "INACTIVE", "ALL").contains(status.toUpperCase(java.util.Locale.ROOT))) {
            throw new IllegalArgumentException("status debe ser ACTIVE, INACTIVE o ALL.");
        }
        Boolean active = null;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            active = true;
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            active = false;
        } // si es ALL o null, active permanece null (todos)

        PagedResult<PatientResponse> paged = patientUseCase.searchPatients(page, size, search, active);
        return ResponseEntity.ok(paged);
    }

    /**
     * Obtiene un paciente por su ID.
     *
     * @param id identificador del paciente
     * @return datos del paciente
     */
    @GetMapping("/{id}")
    public ResponseEntity<PatientResponse> getPatient(@PathVariable Long id) {
        PatientResponse patient = patientUseCase.getPatient(id);
        return ResponseEntity.ok(patient);
    }

    /**
     * Crea un nuevo paciente.
     *
     * @param request datos del paciente a crear
     * @return respuesta con el paciente creado y encabezado Location
     */
    @org.springframework.transaction.annotation.Transactional
    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @org.springframework.validation.annotation.Validated({jakarta.validation.groups.Default.class, PatientRequest.OnCreate.class}) @RequestBody PatientRequest request, java.security.Principal principal, jakarta.servlet.http.HttpServletRequest http) {

        String performedBy = principal.getName();
        String ipAddress = http.getRemoteAddr();
        String userAgent = http.getHeader("User-Agent");

        PatientResponse created = patientUseCase.createPatient(request, performedBy, ipAddress, userAgent);
        URI location = URI.create("/api/v1/patients/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Actualiza un paciente existente.
     *
     * @param id      identificador del paciente
     * @param request datos a actualizar
     * @return paciente actualizado
     */
    @PutMapping("/{id}")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request, java.security.Principal principal, jakarta.servlet.http.HttpServletRequest http) {

        String performedBy = principal.getName();
        String ipAddress = http.getRemoteAddr();
        String userAgent = http.getHeader("User-Agent");

        if (request.version() == null) throw new IllegalArgumentException("Se requiere version para editar.");
        PatientResponse updated = patientUseCase.updatePatient(id, request, performedBy, ipAddress, userAgent);
        return ResponseEntity.ok(updated);
    }

    /**
     * Activa un paciente.
     *
     * @param id identificador del paciente
     * @return respuesta sin contenido
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activatePatient(@PathVariable Long id, @RequestParam Long version, java.security.Principal principal, jakarta.servlet.http.HttpServletRequest http) {
        String performedBy = principal.getName();
        String ipAddress = http.getRemoteAddr();
        String userAgent = http.getHeader("User-Agent");

        patientUseCase.activatePatient(id, version, performedBy, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    /**
     * Desactiva un paciente.
     *
     * @param id identificador del paciente
     * @return respuesta sin contenido
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePatient(@PathVariable Long id, @RequestParam Long version, java.security.Principal principal, jakarta.servlet.http.HttpServletRequest http) {
        String performedBy = principal.getName();
        String ipAddress = http.getRemoteAddr();
        String userAgent = http.getHeader("User-Agent");

        patientUseCase.deactivatePatient(id, version, performedBy, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }
}