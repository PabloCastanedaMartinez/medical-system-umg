package com.cecamed.backend.application.patient.port.out;

import com.cecamed.backend.domain.patient.Patient;
import com.cecamed.backend.application.identity.model.PagedResult;

import java.util.Optional;

/**
 * Puerto de salida para la persistencia de pacientes.
 */
public interface PatientRepositoryPort {

    Optional<Patient> findById(Long id);

    boolean existsByIdentificationNumber(String identificationNumber);

    Patient save(Patient patient);

    PagedResult<Patient> search(int page, int size, String search, Boolean active);
}