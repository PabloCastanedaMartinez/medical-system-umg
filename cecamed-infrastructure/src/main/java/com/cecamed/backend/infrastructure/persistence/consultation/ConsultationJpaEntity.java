package com.cecamed.backend.infrastructure.persistence.consultation;

import com.cecamed.backend.domain.consultation.Consultation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad JPA para la consulta médica.
 */
@Entity
@Table(name = "consultations", schema = "cecamed")
public class ConsultationJpaEntity {

    @Id
    private Long consultationId; // Primary key

    @Column(name = "patient_id", nullable = false)
    private Long patientId; // FK to patient table

    @Column(name = "consultation_date", nullable = false)
    private Instant consultationDate;

    @Column(name = "consultation_type")
    private String consultationType;

    @Column(name = "doctor_name")
    private String doctorName;

    @Column(name = "reason_for_visit")
    private String reasonForVisit;

    @Column(name = "symptoms")
    private String symptoms;

    @Column(name = "examination_findings")
    private String examinationFindings;

    @Column(name = "diagnosis")
    private String diagnosis;

    @Column(name = "treatment_plan")
    private String treatmentPlan;

    @Column(name = "prescriptions")
    private String prescriptions;

    @Column(name = "follow_up_instructions")
    private String followUpInstructions;

    @Column(name = "vital_signs")
    private String vitalSigns;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected ConsultationJpaEntity() {}

    public ConsultationJpaEntity(Long consultationId, Long patientId, Instant consultationDate,
                                 String consultationType, String doctorName, String reasonForVisit,
                                 String symptoms, String examinationFindings, String diagnosis,
                                 String treatmentPlan, String prescriptions, String followUpInstructions,
                                 String vitalSigns, String notes,
                                 Instant createdAt, Instant updatedAt, Long version) {
        this.consultationId = consultationId;
        this.patientId = patientId;
        this.consultationDate = consultationDate;
        this.consultationType = consultationType;
        this.doctorName = doctorName;
        this.reasonForVisit = reasonForVisit;
        this.symptoms = symptoms;
        this.examinationFindings = examinationFindings;
        this.diagnosis = diagnosis;
        this.treatmentPlan = treatmentPlan;
        this.prescriptions = prescriptions;
        this.followUpInstructions = followUpInstructions;
        this.vitalSigns = vitalSigns;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ConsultationJpaEntity fromDomain(Consultation domain) {
        return new ConsultationJpaEntity(
                domain.getConsultationId(),
                domain.getPatientId(),
                domain.getConsultationDate(),
                domain.getConsultationType(),
                domain.getDoctorName(),
                domain.getReasonForVisit(),
                domain.getSymptoms(),
                domain.getExaminationFindings(),
                domain.getDiagnosis(),
                domain.getTreatmentPlan(),
                domain.getPrescriptions(),
                domain.getFollowUpInstructions(),
                domain.getVitalSigns(),
                domain.getNotes(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }

    public Consultation toDomain() {
        return new Consultation(
                this.consultationId,
                this.patientId,
                this.consultationDate,
                this.consultationType,
                this.doctorName,
                this.reasonForVisit,
                this.symptoms,
                this.examinationFindings,
                this.diagnosis,
                this.treatmentPlan,
                this.prescriptions,
                this.followUpInstructions,
                this.vitalSigns,
                this.notes,
                this.createdAt,
                this.updatedAt,
                this.version
        );
    }

    public Long getConsultationId() {
        return consultationId;
    }

    public Long getPatientId() {
        return patientId;
    }

    public Instant getConsultationDate() {
        return consultationDate;
    }

    public String getConsultationType() {
        return consultationType;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public String getReasonForVisit() {
        return reasonForVisit;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public String getExaminationFindings() {
        return examinationFindings;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getTreatmentPlan() {
        return treatmentPlan;
    }

    public String getPrescriptions() {
        return prescriptions;
    }

    public String getFollowUpInstructions() {
        return followUpInstructions;
    }

    public String getVitalSigns() {
        return vitalSigns;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConsultationJpaEntity that = (ConsultationJpaEntity) o;
        return Objects.equals(consultationId, that.consultationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consultationId);
    }
}