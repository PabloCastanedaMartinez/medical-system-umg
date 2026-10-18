package com.cecamed.backend.application.patient.port.in;

import com.cecamed.backend.application.patient.model.PatientRequest;
import com.cecamed.backend.application.patient.model.PatientResponse;
import com.cecamed.backend.application.identity.model.PagedResult;

/**
 * Caso de uso para la gestión de pacientes.
 */
public interface PatientManagementUseCase {

    /**
     * Crea un nuevo paciente.
     *
     * @param request     datos del paciente a crear
     * @param performedBy usuario que realiza la acción
     * @param ipAddress   dirección IP del usuario
     * @param userAgent   agente de usuario del usuario
     * @return datos del paciente creado
     */
    PatientResponse createPatient(PatientRequest request, String performedBy, String ipAddress, String userAgent);

    /**
     * Obtiene un paciente por su ID.
     *
     * @param id identificador del paciente
     * @return datos del paciente
     */
    PatientResponse getPatient(Long id);

    /**
     * Busca pacientes según filtros y paginación.
     *
     * @param page  número de página (comienza en 0)
     * @param size  tamaño de la página
     * @param search término de búsqueda (nombre, apellido, identificación, teléfono, etc.)
     * @param active filtro por estado activo: true para activos, false para inactivos, null para todos
     * @ resultado paginado de pacientes
     */
    PagedResult<PatientResponse> searchPatients(int page, int size, String search, Boolean active);

    /**
     * Actualiza la información de un paciente existente.
     *
     * @param id          identificador del paciente
     * @param request     datos a actualizar
     * @param performedBy usuario que realiza la acción
     * @param ipAddress   dirección IP del usuario
     * @param userAgent   agente de usuario del usuario
     * @return datos del paciente actualizado
     */
    PatientResponse updatePatient(Long id, PatientRequest request, String performedBy, String ipAddress, String userAgent);

    /**
     * Activa un paciente inactivo.
     *
     * @param id          identificador del paciente
     * @param performedBy usuario que realiza la acción
     * @param ipAddress   dirección IP del usuario
     * @param userAgent   agente de usuario del usuario
     */
    void activatePatient(Long id, Long version, String performedBy, String ipAddress, String userAgent);

    /**
     * Desactiva un paciente activo.
     *
     * @param id          identificador del paciente
     * @param performedBy usuario que realiza la acción
     * @param ipAddress   dirección IP del usuario
     * @param userAgent   agente de usuario del usuario
     */
    void deactivatePatient(Long id, Long version, String performedBy, String ipAddress, String userAgent);
}