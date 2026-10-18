package com.cecamed.backend.infrastructure.persistence.record;

import com.cecamed.backend.domain.record.MedicalRecord;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;

/**
 * Entidad JPA para el expediente médico.
 */
@Entity
@Table(name = "medical_records", schema = "cecamed")
public class MedicalRecordJpaEntity {

    @Id
    private Long patientId; // FK to patient table, also serves as primary key

    @Column(nullable = false, unique = true)
    private String recordNumber;

    @Column(name = "allergies")
    private String allergies;

    @Column(name = "pathological_history")
    private String pathologicalHistory;

    @Column(name = "non_pathological_history")
    private String nonPathologicalHistory;

    @Column(name = "family_history")
    private String familyHistory;

    @Column(name = "surgical_history")
    private String surgicalHistory;

    @Column(name = "traumatic_history")
    private String traumaticHistory;

    @Column(name = "current_medications")
    private String currentMedications;

    @Column(name = "general_observations")
    private String generalObservations;

    @Column(name = "gynecological_obstetric_history")
    private String gynecologicalObstetricHistory;

    @Column(name = "water_glasses_per_day")
    private Integer waterGlassesPerDay;

    @Column(name = "meals_per_day")
    private Integer mealsPerDay;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    protected MedicalRecordJpaEntity() {}

    public MedicalRecordJpaEntity(Long patientId, String recordNumber,
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

    public static MedicalRecordJpaEntity fromDomain(MedicalRecord domain) {
        return new MedicalRecordJpaEntity(
                domain.getPatientId(),
                domain.getRecordNumber(),
                domain.getAllergies(),
                domain.getPathologicalHistory(),
                domain.getNonPathologicalHistory(),
                domain.getFamilyHistory(),
                domain.getSurgicalHistory(),
                domain.getTraumaticHistory(),
                domain.getCurrentMedications(),
                domain.getGeneralObservations(),
                domain.getGynecologicalObstetricHistory(),
                domain.getWaterGlassesPerDay(),
                domain.getMealsPerDay(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getVersion()
        );
    }

    public MedicalRecord toDomain() {
        return new MedicalRecord(
                this.patientId,
                this.recordNumber,
                this.allergies,
                this.pathologicalHistory,
                this.nonPathologicalHistory,
                this.familyHistory,
                this.surgicalHistory,
                this.traumaticHistory,
                this.currentMedications,
                this.generalObservations,
                this.gynecologicalObstetricHistory,
                this.waterGlassesPerDay,
                this.mealsPerDay,
                this.createdAt,
                this.updatedAt
        );
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MedicalRecordJpaEntity that = (MedicalRecordJpaEntity) o;
        return Objects.equals(patientId, that.patientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(patientId);
    }
}