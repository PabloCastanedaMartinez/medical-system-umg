package com.cecamed.backend.application.consultation.service;

import com.cecamed.backend.application.consultation.CreateConsultationUseCase;
import com.cecamed.backend.application.consultation.GetConsultationByIdUseCase;
import com.cecamed.backend.application.consultation.GetConsultationsByPatientIdUseCase;
import com.cecamed.backend.application.consultation.UpdateConsultationUseCase;
import com.cecamed.backend.application.consultation.ConsultationRepositoryPort;
import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import java.util.List;

/**
 * Implementación del servicio de consulta médica.
 */
public class ConsultationService implements
        GetConsultationByIdUseCase,
        GetConsultationsByPatientIdUseCase,
        CreateConsultationUseCase,
        UpdateConsultationUseCase {

    private final ConsultationRepositoryPort consultationRepository;

    private final com.cecamed.backend.application.patient.port.out.PatientRepositoryPort patients;
    private final java.time.Clock clock;

    public ConsultationService(ConsultationRepositoryPort consultationRepository,
            com.cecamed.backend.application.patient.port.out.PatientRepositoryPort patients, java.time.Clock clock) {
        this.patients = patients;
        this.clock = clock;
        this.consultationRepository = consultationRepository;
    }

    @Override
    public Consultation getConsultationById(Long consultationId) throws ConsultationNotFoundException {
        return consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));
    }

    @Override
    public List<Consultation> getConsultationsByPatientId(Long patientId) {
        requirePatient(patientId);
        return consultationRepository.findByPatientId(patientId);
    }

    @Override
    public Consultation createConsultation(Consultation consulta) {
        var patient = requirePatient(consulta.getPatientId());
        if (!patient.isActive()) throw new com.cecamed.backend.domain.patient.PatientInactiveException("Paciente inactivo.");
        consulta.initialize(clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        return consultationRepository.save(consulta);
    }

    @Override
    public Consultation updateConsultation(Long consultationId, Consultation consulta) {
        // Verify that the consultation exists
        Consultation existing = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        if (!existing.getPatientId().equals(consulta.getPatientId())) throw new ConsultationNotFoundException(consultationId);
        if (consulta.getVersion() == null || !consulta.getVersion().equals(existing.getVersion())) {
            throw new com.cecamed.backend.domain.patient.StaleVersionException("La consulta cambió. Recargue antes de editar.");
        }
        // Update the fields (keeping the same consultationId and patientId which are immutable)
        Consultation updated = new Consultation(
                existing.getConsultationId(),
                existing.getPatientId(),
                existing.getConsultationDate(),
                consulta.getConsultationType(),
                consulta.getDoctorName(),
                consulta.getReasonForVisit(),
                consulta.getSymptoms(),
                consulta.getExaminationFindings(),
                consulta.getDiagnosis(),
                consulta.getTreatmentPlan(),
                consulta.getPrescriptions(),
                consulta.getFollowUpInstructions(),
                consulta.getVitalSigns(),
                consulta.getNotes(),
                existing.getCreatedAt(), // Keep original createdAt
                clock.instant().truncatedTo(java.time.temporal.ChronoUnit.MICROS),
                existing.getVersion() // Keep existing version - will be incremented by JPA
        );

        updated.setIcd10Code(consulta.getIcd10Code());
        return consultationRepository.save(updated);
    }
    private com.cecamed.backend.domain.patient.Patient requirePatient(Long id) {
        return patients.findById(id).orElseThrow(() -> new com.cecamed.backend.application.shared.ResourceNotFoundException("Paciente no encontrado."));
    }
}
