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
    public ResponseEntity<PatientResponse[]> listPatients(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {

        Boolean active = null;
        if ("ACTIVE".equalsIgnoreCase(status)) {
            active = true;
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            active = false;
        } // si es ALL o null, active permanece null (todos)

        // Usamos un tamaño de página grande para obtener todos los resultados (hasta el límite del servicio)
        PagedResult<PatientResponse> paged = patientUseCase.searchPatients(0, 10000, search, active);
        PatientResponse[] patients = paged.content().toArray(new PatientResponse[0]);
        return ResponseEntity.ok(patients);
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
    @PostMapping
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody PatientRequest request) {

        // En una aplicación real, obtendríamos el usuario y la IP del contexto de seguridad.
        // Por simplicidad, usamos valores placeholder.
        String performedBy = "system"; // TODO: obtener de SecurityContext
        String ipAddress = "0.0.0.0"; // TODO: obtener de HttpServletRequest
        String userAgent = "unknown"; // TODO: obtener de HttpServletRequest

        PatientResponse created = patientUseCase.createPatient(request, performedBy, ipAddress, userAgent);
        URI location = URI.create("/api/v1/patients/" + created.getId());
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
            @Valid @RequestBody PatientRequest request) {

        String performedBy = "system";
        String ipAddress = "0.0.0.0";
        String userAgent = "unknown";

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
    public ResponseEntity<Void> activatePatient(@PathVariable Long id) {
        String performedBy = "system";
        String ipAddress = "0.0.0.0";
        String userAgent = "unknown";

        patientUseCase.activatePatient(id, performedBy, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }

    /**
     * Desactiva un paciente.
     *
     * @param id identificador del paciente
     * @return respuesta sin contenido
     */
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivatePatient(@PathVariable Long id) {
        String performedBy = "system";
        String ipAddress = "0.0.0.0";
        String userAgent = "unknown";

        patientUseCase.deactivatePatient(id, performedBy, ipAddress, userAgent);
        return ResponseEntity.noContent().build();
    }
}