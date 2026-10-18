package com.cecamed.backend.domain.record;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the MedicalRecord domain entity.
 */
class MedicalRecordTest {

    @Test
    void shouldCreateMedicalRecordWithAllFields() {
        // Given
        Long patientId = 1L;
        String recordNumber = "MR000123";
        String allergies = "Penicillin";
        String pathologicalHistory = "Hypertension";
        String nonPathologicalHistory = "None";
        String familyHistory = "Diabetes in father";
        String surgicalHistory = "Appendectomy";
        String traumaticHistory = "None";
        String currentMedications = "Lisinopril";
        String generalObservations = "Patient in good health";
        String gynecologicalObstetricHistory = "G2P2";
        Integer waterGlassesPerDay = 8;
        Integer mealsPerDay = 3;
        Instant createdAt = Instant.parse("2026-10-15T09:30:00Z");
        Instant updatedAt = Instant.parse("2026-10-16T10:45:00Z");

        // When
        MedicalRecord medicalRecord = new MedicalRecord(
                patientId, recordNumber,
                allergies, pathologicalHistory, nonPathologicalHistory,
                familyHistory, surgicalHistory, traumaticHistory,
                currentMedications, generalObservations,
                gynecologicalObstetricHistory,
                waterGlassesPerDay, mealsPerDay,
                createdAt, updatedAt);

        // Then
        assertThat(medicalRecord.getPatientId()).isEqualTo(patientId);
        assertThat(medicalRecord.getRecordNumber()).isEqualTo(recordNumber);
        assertThat(medicalRecord.getAllergies()).isEqualTo(allergies);
        assertThat(medicalRecord.getPathologicalHistory()).isEqualTo(pathologicalHistory);
        assertThat(medicalRecord.getNonPathologicalHistory()).isEqualTo(nonPathologicalHistory);
        assertThat(medicalRecord.getFamilyHistory()).isEqualTo(familyHistory);
        assertThat(medicalRecord.getSurgicalHistory()).isEqualTo(surgicalHistory);
        assertThat(medicalRecord.getTraumaticHistory()).isEqualTo(traumaticHistory);
        assertThat(medicalRecord.getCurrentMedications()).isEqualTo(currentMedications);
        assertThat(medicalRecord.getGeneralObservations()).isEqualTo(generalObservations);
        assertThat(medicalRecord.getGynecologicalObstetricHistory()).isEqualTo(gynecologicalObstetricHistory);
        assertThat(medicalRecord.getWaterGlassesPerDay()).isEqualTo(waterGlassesPerDay);
        assertThat(medicalRecord.getMealsPerDay()).isEqualTo(mealsPerDay);
        assertThat(medicalRecord.getCreatedAt()).isEqualTo(createdAt);
        assertThat(medicalRecord.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(medicalRecord.getVersion()).isEqualTo(0L); // Default version for new records
    }

    @Test
    void shouldCreateMedicalRecordWithSpecifiedVersion() {
        // Given
        Long patientId = 1L;
        String recordNumber = "MR000123";
        Instant createdAt = Instant.parse("2026-10-15T09:30:00Z");
        Instant updatedAt = Instant.parse("2026-10-16T10:45:00Z");

        // When
        MedicalRecord medicalRecord = new MedicalRecord(
                patientId, recordNumber,
                "Allergies", "Patho", "Non-patho",
                "Family", "Surgical", "Traumatic",
                "Meds", "Observations",
                "Gyn-Obs",
                8, 3,
                createdAt, updatedAt,
                5L); // Specified version

        // Then
        assertThat(medicalRecord.getVersion()).isEqualTo(5L);
    }

    @Test
    void shouldHandleNullValuesForOptionalFields() {
        // Given
        Long patientId = 1L;
        String recordNumber = "MR000123";

        // When
        MedicalRecord medicalRecord = new MedicalRecord(
                patientId, recordNumber,
                null, null, null,
                null, null, null,
                null, null,
                null,
                null, null,
                Instant.now(), Instant.now());

        // Then
        assertThat(medicalRecord.getAllergies()).isNull();
        assertThat(medicalRecord.getPathologicalHistory()).isNull();
        assertThat(medicalRecord.getNonPathologicalHistory()).isNull();
        assertThat(medicalRecord.getFamilyHistory()).isNull();
        assertThat(medicalRecord.getSurgicalHistory()).isNull();
        assertThat(medicalRecord.getTraumaticHistory()).isNull();
        assertThat(medicalRecord.getCurrentMedications()).isNull();
        assertThat(medicalRecord.getGeneralObservations()).isNull();
        assertThat(medicalRecord.getGynecologicalObstetricHistory()).isNull();
        assertThat(medicalRecord.getWaterGlassesPerDay()).isNull();
        assertThat(medicalRecord.getMealsPerDay()).isNull();
    }

    @Test
    void shouldAllowUpdatingUpdatableFields() {
        // Given
        MedicalRecord medicalRecord = new MedicalRecord(
                1L, "MR000123",
                "Old allergies", "Old patho", "Old non-patho",
                "Old family", "Old surgical", "Old traumatic",
                "Old meds", "Old observations",
                "Old gyn-obs",
                5, 2,
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"));

        // When
        medicalRecord.setAllergies("New allergies");
        medicalRecord.setPathologicalHistory("New patho");
        medicalRecord.setUpdatedAt(Instant.parse("2026-10-16T10:45:00Z"));
        medicalRecord.setVersion(1L);

        // Then
        assertThat(medicalRecord.getAllergies()).isEqualTo("New allergies");
        assertThat(medicalRecord.getPathologicalHistory()).isEqualTo("New patho");
        assertThat(medicalRecord.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-16T10:45:00Z"));
        assertThat(medicalRecord.getVersion()).isEqualTo(1L);
        // Immutable fields should remain unchanged
        assertThat(medicalRecord.getPatientId()).isEqualTo(1L);
        assertThat(medicalRecord.getRecordNumber()).isEqualTo("MR000123");
        assertThat(medicalRecord.getCreatedAt()).isEqualTo(Instant.parse("2026-10-15T09:30:00Z"));
    }

    @Test
    void shouldBeEqualByPatientIdOnly() {
        // Given
        MedicalRecord record1 = new MedicalRecord(
                1L, "MR0001",
                "Allergies", "Patho", "Non-patho",
                "Family", "Surgical", "Traumatic",
                "Meds", "Observations",
                "Gyn-Obs",
                8, 3,
                Instant.now(), Instant.now(),
                0L);

        MedicalRecord record2 = new MedicalRecord(
                1L, "MR0002", // Same patientId, different recordNumber
                "Different allergies", "Different patho", "Different non-patho",
                "Different family", "Different surgical", "Different traumatic",
                "Different meds", "Different observations",
                "Different gyn-obs",
                5, 2,
                Instant.now(), Instant.now(),
                0L);

        MedicalRecord record3 = new MedicalRecord(
                2L, "MR0001", // Different patientId, same recordNumber
                "Allergies", "Patho", "Non-patho",
                "Family", "Surgical", "Traumatic",
                "Meds", "Observations",
                "Gyn-Obs",
                8, 3,
                Instant.now(), Instant.now(),
                0L);

        // Then
        assertThat(record1).isEqualTo(record2); // Same patientId
        assertThat(record1).isNotEqualTo(record3); // Different patientId
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
        assertThat(record1.hashCode()).isNotEqualTo(record3.hashCode());
    }
}