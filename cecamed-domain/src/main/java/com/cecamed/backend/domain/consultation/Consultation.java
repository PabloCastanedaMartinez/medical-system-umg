package com.cecamed.backend.domain.consultation;

import java.time.Instant;
import java.util.Objects;

/**
 * Consulta médica de un paciente.
 * Un paciente puede tener múltiples consultas a lo largo del tiempo.
 */
public class Consultation {

    private Long consultationId;
    private Long patientId; // FK to patient table
    private Instant consultationDate;
    private String consultationType;
    private String doctorName;
    private String reasonForVisit;
    private String symptoms;
    private String examinationFindings;
    private String diagnosis;
    private String treatmentPlan;
    private String prescriptions;
    private String followUpInstructions;
    private String vitalSigns;
    private String notes;
    private String icd10Code;
    public String getIcd10Code() { return icd10Code; }
    public void setIcd10Code(String value) { icd10Code = value; }
    private Instant createdAt;
    private Instant updatedAt;
    private Long version; // For optimistic locking

    protected Consultation() {}

    public Consultation(Long consultationId, Long patientId, Instant consultationDate,
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

    // Constructor for creating new consultations (without ID and version)
    public Consultation(Long patientId, Instant consultationDate,
                        String consultationType, String doctorName, String reasonForVisit,
                        String symptoms, String examinationFindings, String diagnosis,
                        String treatmentPlan, String prescriptions, String followUpInstructions,
                        String vitalSigns, String notes,
                        Instant createdAt, Instant updatedAt) {
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
        this.version = 0L; // Initial version for new consultations
    }

    public void initialize(java.time.Instant now) {
        consultationDate = now;
        createdAt = now;
        updatedAt = now;
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

    // Setters for fields that can be updated (excluding consultationId and patientId which are immutable)
    public void setConsultationDate(Instant consultationDate) {
        this.consultationDate = consultationDate;
    }

    public void setConsultationType(String consultationType) {
        this.consultationType = consultationType;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public void setReasonForVisit(String reasonForVisit) {
        this.reasonForVisit = reasonForVisit;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
    }

    public void setExaminationFindings(String examinationFindings) {
        this.examinationFindings = examinationFindings;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public void setTreatmentPlan(String treatmentPlan) {
        this.treatmentPlan = treatmentPlan;
    }

    public void setPrescriptions(String prescriptions) {
        this.prescriptions = prescriptions;
    }

    public void setFollowUpInstructions(String followUpInstructions) {
        this.followUpInstructions = followUpInstructions;
    }

    public void setVitalSigns(String vitalSigns) {
        this.vitalSigns = vitalSigns;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Consultation that = (Consultation) o;
        return Objects.equals(consultationId, that.consultationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(consultationId);
    }

    @Override
    public String toString() {
        return "Consultation{" +
                "consultationId=" + consultationId +
                ", patientId=" + patientId +
                ", consultationDate=" + consultationDate +
                '}';
    }
}