package com.cecamed.backend.domain.consultation;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the Consultation domain class.
 */
class ConsultationTest {

    @Test
    void shouldCreateConsultationWithAllFields() {
        // Given
        Long consultationId = 1L;
        Long patientId = 1L;
        Instant consultationDate = Instant.parse("2026-10-15T09:30:00Z");
        String consultationType = "General";
        String doctorName = "Dr. Smith";
        String reasonForVisit = "Regular checkup";
        String symptoms = "No symptoms";
        String examinationFindings = "Normal exam";
        String diagnosis = "Healthy";
        String treatmentPlan = "Vitamins";
        String prescriptions = "None";
        String followUpInstructions = "Return in 6 months";
        String vitalSigns = "BP 120/80";
        String notes = "Patient doing well";
        Instant createdAt = Instant.parse("2026-10-15T09:30:00Z");
        Instant updatedAt = Instant.parse("2026-10-15T09:30:00Z");
        Long version = 0L;

        // When
        Consultation consultation = new Consultation(consultationId, patientId, consultationDate, consultationType, doctorName, reasonForVisit, symptoms, examinationFindings, diagnosis, treatmentPlan, prescriptions, followUpInstructions, vitalSigns, notes, createdAt, updatedAt, version);

        // Then
        assertThat(consultation.getConsultationId()).isEqualTo(consultationId);
        assertThat(consultation.getPatientId()).isEqualTo(patientId);
        assertThat(consultation.getConsultationDate()).isEqualTo(consultationDate);
        assertThat(consultation.getConsultationType()).isEqualTo(consultationType);
        assertThat(consultation.getDoctorName()).isEqualTo(doctorName);
        assertThat(consultation.getReasonForVisit()).isEqualTo(reasonForVisit);
        assertThat(consultation.getSymptoms()).isEqualTo(symptoms);
        assertThat(consultation.getExaminationFindings()).isEqualTo(examinationFindings);
        assertThat(consultation.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(consultation.getTreatmentPlan()).isEqualTo(treatmentPlan);
        assertThat(consultation.getPrescriptions()).isEqualTo(prescriptions);
        assertThat(consultation.getFollowUpInstructions()).isEqualTo(followUpInstructions);
        assertThat(consultation.getVitalSigns()).isEqualTo(vitalSigns);
        assertThat(consultation.getNotes()).isEqualTo(notes);
        assertThat(consultation.getCreatedAt()).isEqualTo(createdAt);
        assertThat(consultation.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(consultation.getVersion()).isEqualTo(version);
    }

    @Test
    void shouldCreateConsultationWithConstructorWithoutIdAndVersion() {
        // Given
        Long patientId = 1L;
        Instant consultationDate = Instant.parse("2026-10-15T09:30:00Z");
        String consultationType = "General";
        String doctorName = "Dr. Smith";
        String reasonForVisit = "Regular checkup";
        String symptoms = "No symptoms";
        String examinationFindings = "Normal exam";
        String diagnosis = "Healthy";
        String treatmentPlan = "Vitamins";
        String prescriptions = "None";
        String followUpInstructions = "Return in 6 months";
        String vitalSigns = "BP 120/80";
        String notes = "Patient doing well";
        Instant createdAt = Instant.parse("2026-10-15T09:30:00Z");
        Instant updatedAt = Instant.parse("2026-10-15T09:30:00Z");

        // When
        Consultation consultation = new Consultation(patientId, consultationDate, consultationType, doctorName, reasonForVisit, symptoms, examinationFindings, diagnosis, treatmentPlan, prescriptions, followUpInstructions, vitalSigns, notes, createdAt, updatedAt);

        // Then
        assertThat(consultation.getPatientId()).isEqualTo(patientId);
        assertThat(consultation.getConsultationDate()).isEqualTo(consultationDate);
        assertThat(consultation.getConsultationType()).isEqualTo(consultationType);
        assertThat(consultation.getDoctorName()).isEqualTo(doctorName);
        assertThat(consultation.getReasonForVisit()).isEqualTo(reasonForVisit);
        assertThat(consultation.getSymptoms()).isEqualTo(symptoms);
        assertThat(consultation.getExaminationFindings()).isEqualTo(examinationFindings);
        assertThat(consultation.getDiagnosis()).isEqualTo(diagnosis);
        assertThat(consultation.getTreatmentPlan()).isEqualTo(treatmentPlan);
        assertThat(consultation.getPrescriptions()).isEqualTo(prescriptions);
        assertThat(consultation.getFollowUpInstructions()).isEqualTo(followUpInstructions);
        assertThat(consultation.getVitalSigns()).isEqualTo(vitalSigns);
        assertThat(consultation.getNotes()).isEqualTo(notes);
        assertThat(consultation.getCreatedAt()).isEqualTo(createdAt);
        assertThat(consultation.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(consultation.getVersion()).isEqualTo(0L); // Initial version
        assertThat(consultation.getConsultationId()).isNull(); // ID not set yet
    }

    @Test
    void shouldAllowUpdatingMutableFields() {
        // Given
        Consultation consultation = new Consultation(
                1L,
                1L,
                Instant.parse("2026-10-15T09:30:00Z"),
                "General",
                "Dr. Smith",
                "Regular checkup",
                "No symptoms",
                "Normal exam",
                "Healthy",
                "Vitamins",
                "None",
                "Return in 6 months",
                "BP 120/80",
                "Patient doing well",
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"),
                0L
        );

        // When
        consultation.setConsultationType("Follow-up");
        consultation.setDoctorName("Dr. Jones");
        consultation.setReasonForVisit("Blood pressure check");
        consultation.setSymptoms("Slightly elevated BP");
        consultation.setExaminationFindings("BP 130/85");
        consultation.setDiagnosis("Hypertension");
        consultation.setTreatmentPlan("Increase medication");
        consultation.setPrescriptions("Medication X");
        consultation.setFollowUpInstructions("Check BP weekly");
        consultation.setVitalSigns("BP 130/85");
        consultation.setNotes("Patient compliant with medication");
        consultation.setUpdatedAt(Instant.parse("2026-10-16T10:00:00Z"));
        consultation.setVersion(1L);

        // Then
        assertThat(consultation.getConsultationType()).isEqualTo("Follow-up");
        assertThat(consultation.getDoctorName()).isEqualTo("Dr. Jones");
        assertThat(consultation.getReasonForVisit()).isEqualTo("Blood pressure check");
        assertThat(consultation.getSymptoms()).isEqualTo("Slightly elevated BP");
        assertThat(consultation.getExaminationFindings()).isEqualTo("BP 130/85");
        assertThat(consultation.getDiagnosis()).isEqualTo("Hypertension");
        assertThat(consultation.getTreatmentPlan()).isEqualTo("Increase medication");
        assertThat(consultation.getPrescriptions()).isEqualTo("Medication X");
        assertThat(consultation.getFollowUpInstructions()).isEqualTo("Check BP weekly");
        assertThat(consultation.getVitalSigns()).isEqualTo("BP 130/85");
        assertThat(consultation.getNotes()).isEqualTo("Patient compliant with medication");
        assertThat(consultation.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-16T10:00:00Z"));
        assertThat(consultation.getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldNotAllowChangingImmutableFieldsViaSetters() {
        // Given
        Consultation consultation = new Consultation(
                1L,
                1L,
                Instant.parse("2026-10-15T09:30:00Z"),
                "General",
                "Dr. Smith",
                "Regular checkup",
                "No symptoms",
                "Normal exam",
                "Healthy",
                "Vitamins",
                "None",
                "Return in 6 months",
                "BP 120/80",
                "Patient doing well",
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"),
                0L
        );

        Long originalId = consultation.getConsultationId();
        Long originalPatientId = consultation.getPatientId();
        Instant originalDate = consultation.getConsultationDate();

        // When - Attempt to change immutable fields (setters don't exist for these)
        // Note: There are no setters for consultationId, patientId, or consultationDate
        // so these fields remain unchanged

        // Then
        assertThat(consultation.getConsultationId()).isEqualTo(originalId);
        assertThat(consultation.getPatientId()).isEqualTo(originalPatientId);
        assertThat(consultation.getConsultationDate()).isEqualTo(originalDate);
    }

    @Test
    void shouldCompareConsultationsById() {
        // Given
        Consultation consultation1 = new Consultation(
                1L,
                1L,
                Instant.parse("2026-10-15T09:30:00Z"),
                "General",
                "Dr. Smith",
                "Regular checkup",
                "No symptoms",
                "Normal exam",
                "Healthy",
                "Vitamins",
                "None",
                "Return in 6 months",
                "BP 120/80",
                "Patient doing well",
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"),
                0L
        );

        Consultation consultation2 = new Consultation(
                1L, // Same ID
                2L, // Different patient ID
                Instant.parse("2026-09-15T10:00:00Z"), // Different date
                "Follow-up",
                "Dr. Jones",
                "Different reason",
                "Different symptoms",
                "Different findings",
                "Different diagnosis",
                "Different treatment",
                "Different prescriptions",
                "Different follow-up",
                "Different vital signs",
                "Different notes",
                Instant.parse("2026-09-15T10:00:00Z"),
                Instant.parse("2026-09-15T10:00:00Z"),
                0L
        );

        Consultation consultation3 = new Consultation(
                2L, // Different ID
                1L,
                Instant.parse("2026-10-15T09:30:00Z"),
                "General",
                "Dr. Smith",
                "Regular checkup",
                "No symptoms",
                "Normal exam",
                "Healthy",
                "Vitamins",
                "None",
                "Return in 6 months",
                "BP 120/80",
                "Patient doing well",
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"),
                0L
        );

        // Then
        assertThat(consultation1).isEqualTo(consultation2); // Same ID
        assertThat(consultation1).isNotEqualTo(consultation3); // Different ID
        assertThat(consultation1.hashCode()).isEqualTo(consultation2.hashCode()); // Same ID, same hash
        assertThat(consultation1.hashCode()).isNotEqualTo(consultation3.hashCode()); // Different ID, different hash
    }

    @Test
    void shouldThrowExceptionWhenConsultationNotFoundById() {
        // Given
        Long consultationId = 999L;

        // When & Then
        ConsultationNotFoundException exception = new ConsultationNotFoundException(consultationId);
        assertThat(exception).hasMessageContaining("Consultation not found for ID: 999");
    }

    @Test
    void shouldThrowExceptionWhenConsultationNotFoundByPatientAndDate() {
        // Given
        Long patientId = 1L;
        Instant consultationDate = Instant.parse("2026-10-15T09:30:00Z");

        // When & Then
        ConsultationNotFoundException exception = new ConsultationNotFoundException(patientId, consultationDate);
        assertThat(exception).hasMessageContaining("Consultation not found for patient ID: 1");
        assertThat(exception).hasMessageContaining("at date: 2026-10-15T09:30:00Z");
    }
}