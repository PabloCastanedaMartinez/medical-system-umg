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

    public ConsultationService(ConsultationRepositoryPort consultationRepository) {
        this.consultationRepository = consultationRepository;
    }

    @Override
    public Consultation getConsultationById(Long consultationId) throws ConsultationNotFoundException {
        return consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));
    }

    @Override
    public List<Consultation> getConsultationsByPatientId(Long patientId) {
        return consultationRepository.findByPatientId(patientId);
    }

    @Override
    public Consultation createConsultation(Consultation consulta) {
        return consultationRepository.save(consulta);
    }

    @Override
    public Consultation updateConsultation(Long consultationId, Consultation consulta) {
        // Verify that the consultation exists
        Consultation existing = consultationRepository.findById(consultationId)
                .orElseThrow(() -> new ConsultationNotFoundException(consultationId));

        // Update the fields (keeping the same consultationId and patientId which are immutable)
        Consultation updated = new Consultation(
                existing.getConsultationId(),
                existing.getPatientId(),
                consulta.getConsultationDate() != null ? consulta.getConsultationDate() : existing.getConsultationDate(),
                consulta.getConsultationType() != null ? consulta.getConsultationType() : existing.getConsultationType(),
                consulta.getDoctorName() != null ? consulta.getDoctorName() : existing.getDoctorName(),
                consulta.getReasonForVisit() != null ? consulta.getReasonForVisit() : existing.getReasonForVisit(),
                consulta.getSymptoms() != null ? consulta.getSymptoms() : existing.getSymptoms(),
                consulta.getExaminationFindings() != null ? consulta.getExaminationFindings() : existing.getExaminationFindings(),
                consulta.getDiagnosis() != null ? consulta.getDiagnosis() : existing.getDiagnosis(),
                consulta.getTreatmentPlan() != null ? consulta.getTreatmentPlan() : existing.getTreatmentPlan(),
                consulta.getPrescriptions() != null ? consulta.getPrescriptions() : existing.getPrescriptions(),
                consulta.getFollowUpInstructions() != null ? consulta.getFollowUpInstructions() : existing.getFollowUpInstructions(),
                consulta.getVitalSigns() != null ? consulta.getVitalSigns() : existing.getVitalSigns(),
                consulta.getNotes() != null ? consulta.getNotes() : existing.getNotes(),
                existing.getCreatedAt(), // Keep original createdAt
                consulta.getUpdatedAt() != null ? consulta.getUpdatedAt() : existing.getUpdatedAt(), // Set new updatedAt
                existing.getVersion() // Keep existing version - will be incremented by JPA
        );

        return consultationRepository.save(updated);
    }
}