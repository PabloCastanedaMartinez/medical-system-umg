package com.cecamed.backend.domain.record;

import java.time.Instant;
import java.util.Objects;

/**
 * Expediente clínico de un paciente.
 * Un paciente tiene exactamente un expediente.
 */
public class MedicalRecord {

    private Long patientId; // FK to patient table
    private String recordNumber;
    private String allergies;
    private String pathologicalHistory;
    private String nonPathologicalHistory;
    private String familyHistory;
    private String surgicalHistory;
    private String traumaticHistory;
    private String currentMedications;
    private String generalObservations;
    private String gynecologicalObstetricHistory;
    private Integer waterGlassesPerDay;
    private Integer mealsPerDay;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version; // For optimistic locking

    protected MedicalRecord() {}

    public MedicalRecord(Long patientId, String recordNumber,
                         String allergies, String pathologicalHistory, String nonPathologicalHistory,
                         String familyHistory, String surgicalHistory, String traumaticHistory,
                         String currentMedications, String generalObservations,
                         String gynecologicalObstetricHistory,
                         Integer waterGlassesPerDay, Integer mealsPerDay,
                         Instant createdAt, Instant updatedAt, Long version) {
        this.patientId = patientId;
        this.recordNumber = recordNumber;
        this.allergies = allergies;
        this.pathologicalHistory = pathologicalHistory;
        this.nonPathologicalHistory = nonPathologicalHistory;
        this.familyHistory = familyHistory;
        this.surgicalHistory = surgicalHistory;
        this.traumaticHistory = traumaticHistory;
        this.currentMedications = currentMedications;
        this.generalObservations = generalObservations;
        this.gynecologicalObstetricHistory = gynecologicalObstetricHistory;
        this.waterGlassesPerDay = waterGlassesPerDay;
        this.mealsPerDay = mealsPerDay;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    // Constructor without version for creating new records
    public MedicalRecord(Long patientId, String recordNumber,
                         String allergies, String pathologicalHistory, String nonPathologicalHistory,
                         String familyHistory, String surgicalHistory, String traumaticHistory,
                         String currentMedications, String generalObservations,
                         String gynecologicalObstetricHistory,
                         Integer waterGlassesPerDay, Integer mealsPerDay,
                         Instant createdAt, Instant updatedAt) {
        this.patientId = patientId;
        this.recordNumber = recordNumber;
        this.allergies = allergies;
        this.pathologicalHistory = pathologicalHistory;
        this.nonPathologicalHistory = nonPathologicalHistory;
        this.familyHistory = familyHistory;
        this.surgicalHistory = surgicalHistory;
        this.traumaticHistory = traumaticHistory;
        this.currentMedications = currentMedications;
        this.generalObservations = generalObservations;
        this.gynecologicalObstetricHistory = gynecologicalObstetricHistory;
        this.waterGlassesPerDay = waterGlassesPerDay;
        this.mealsPerDay = mealsPerDay;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = 0L; // Initial version for new records
    }

    public Long getPatientId() {
        return patientId;
    }

    public String getRecordNumber() {
        return recordNumber;
    }

    public String getAllergies() {
        return allergies;
    }

    public String getPathologicalHistory() {
        return pathologicalHistory;
    }

    public String getNonPathologicalHistory() {
        return nonPathologicalHistory;
    }

    public String getFamilyHistory() {
        return familyHistory;
    }

    public String getSurgicalHistory() {
        return surgicalHistory;
    }

    public String getTraumaticHistory() {
        return traumaticHistory;
    }

    public String getCurrentMedications() {
        return currentMedications;
    }

    public String getGeneralObservations() {
        return generalObservations;
    }

    public String getGynecologicalObstetricHistory() {
        return gynecologicalObstetricHistory;
    }

    public Integer getWaterGlassesPerDay() {
        return waterGlassesPerDay;
    }

    public Integer getMealsPerDay() {
        return mealsPerDay;
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

    // Setters for fields that can be updated (excluding patientId and recordNumber which are immutable)
    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public void setPathologicalHistory(String pathologicalHistory) {
        this.pathologicalHistory = pathologicalHistory;
    }

    public void setNonPathologicalHistory(String nonPathologicalHistory) {
        this.nonPathologicalHistory = nonPathologicalHistory;
    }

    public void setFamilyHistory(String familyHistory) {
        this.familyHistory = familyHistory;
    }

    public void setSurgicalHistory(String surgicalHistory) {
        this.surgicalHistory = surgicalHistory;
    }

    public void setTraumaticHistory(String traumaticHistory) {
        this.traumaticHistory = traumaticHistory;
    }

    public void setCurrentMedications(String currentMedications) {
        this.currentMedications = currentMedications;
    }

    public void setGeneralObservations(String generalObservations) {
        this.generalObservations = generalObservations;
    }

    public void setGynecologicalObstetricHistory(String gynecologicalObstetricHistory) {
        this.gynecologicalObstetricHistory = gynecologicalObstetricHistory;
    }

    public void setWaterGlassesPerDay(Integer waterGlassesPerDay) {
        this.waterGlassesPerDay = waterGlassesPerDay;
    }

    public void setMealsPerDay(Integer mealsPerDay) {
        this.mealsPerDay = mealsPerDay;
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
        MedicalRecord that = (MedicalRecord) o;
        return Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }

    @Override
    public String toString() {
        return "MedicalRecord{" +
                "patientId=" + patientId +
                ", recordNumber='" + recordNumber + '\'' +
                '}';
    }
}