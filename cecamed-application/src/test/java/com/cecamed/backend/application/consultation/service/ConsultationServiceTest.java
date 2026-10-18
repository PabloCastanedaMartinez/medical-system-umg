package com.cecamed.backend.application.consultation.service;

import com.cecamed.backend.application.consultation.CreateConsultationUseCase;
import com.cecamed.backend.application.consultation.GetConsultationByIdUseCase;
import com.cecamed.backend.application.consultation.GetConsultationsByPatientIdUseCase;
import com.cecamed.backend.application.consultation.UpdateConsultationUseCase;
import com.cecamed.backend.application.consultation.service.ConsultationService;
import com.cecamed.backend.application.consultation.ConsultationRepositoryPort;
import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the ConsultationService.
 */
class ConsultationServiceTest {

    @Mock
    private ConsultationRepositoryPort consultationRepository;

    private ConsultationService consultationService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        consultationService = new ConsultationService(consultationRepository);
    }

    @Test
    void shouldGetConsultationByIdWhenExists() {
        // Given
        Long consultationId = 1L;
        Consultation expectedConsultation = createTestConsultation(consultationId);
        when(consultationRepository.findById(consultationId))
                .thenReturn(Optional.of(expectedConsultation));

        // When
        Consultation result = consultationService.getConsultationById(consultationId);

        // Then
        assertThat(result).isEqualTo(expectedConsultation);
        verify(consultationRepository).findById(consultationId);
    }

    @Test
    void shouldThrowExceptionWhenConsultationNotFoundById() {
        // Given
        Long consultationId = 1L;
        when(consultationRepository.findById(consultationId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> consultationService.getConsultationById(consultationId))
                .isInstanceOf(ConsultationNotFoundException.class)
                .hasMessageContaining("Consultation not found for ID: " + consultationId);
        verify(consultationRepository).findById(consultationId);
    }

    @Test
    void shouldGetConsultationsByPatientIdWhenExist() {
        // Given
        Long patientId = 1L;
        Consultation consultation1 = createTestConsultation(1L);
        consultation1.setConsultationDate(Instant.parse("2026-10-15T09:30:00Z"));
        Consultation consultation2 = createTestConsultation(2L);
        consultation2.setConsultationDate(Instant.parse("2026-09-15T14:00:00Z")); // Older date

        List<Consultation> expectedConsultations = List.of(consultation1, consultation2); // Most recent first
        when(consultationRepository.findByPatientId(patientId))
                .thenReturn(expectedConsultations);

        // When
        List<Consultation> result = consultationService.getConsultationsByPatientId(patientId);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getConsultationId()).isEqualTo(1L); // Most recent first
        assertThat(result.get(1).getConsultationId()).isEqualTo(2L);
        verify(consultationRepository).findByPatientId(patientId);
    }

    @Test
    void shouldReturnEmptyListWhenPatientHasNoConsultations() {
        // Given
        Long patientId = 1L;
        when(consultationRepository.findByPatientId(patientId))
                .thenReturn(List.of());

        // When
        List<Consultation> result = consultationService.getConsultationsByPatientId(patientId);

        // Then
        assertThat(result).isEmpty();
        verify(consultationRepository).findByPatientId(patientId);
    }

    @Test
    void shouldCreateConsultationSuccessfully() {
        // Given
        Consultation consultationToCreate = new Consultation(
                1L, // patientId
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
                Instant.parse("2026-10-15T09:30:00Z")
        );
        // New consultation doesn't have ID yet (ID will be assigned by repository)
        Consultation expectedConsultation = new Consultation(
                1L, // consultationId (will be set by repository)
                1L, // patientId
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
                0L // initial version
        );

        when(consultationRepository.save(any(Consultation.class)))
                .thenAnswer(invocation -> {
                    Consultation saved = invocation.getArgument(0);
                    // When saving a new consultation, the repository assigns the ID and version
                    // We simulate this by setting them on the saved object
                    Consultation resultWithId = new Consultation(
                            1L, // assigned ID
                            saved.getPatientId(),
                            saved.getConsultationDate(),
                            saved.getConsultationType(),
                            saved.getDoctorName(),
                            saved.getReasonForVisit(),
                            saved.getSymptoms(),
                            saved.getExaminationFindings(),
                            saved.getDiagnosis(),
                            saved.getTreatmentPlan(),
                            saved.getPrescriptions(),
                            saved.getFollowUpInstructions(),
                            saved.getVitalSigns(),
                            saved.getNotes(),
                            saved.getCreatedAt(),
                            saved.getUpdatedAt(),
                            0L // assigned version
                    );
                    return resultWithId;
                });

        // When
        Consultation result = consultationService.createConsultation(consultationToCreate);

        // Then
        assertThat(result.getConsultationId()).isEqualTo(1L);
        assertThat(result.getVersion()).isEqualTo(0L);
        verify(consultationRepository).save(any(Consultation.class));
    }

    @Test
    void shouldUpdateConsultationSuccessfully() {
        // Given
        Long consultationId = 1L;
        Consultation existingConsultation = createTestConsultation(consultationId);
        existingConsultation.setConsultationType("Follow-up");
        existingConsultation.setUpdatedAt(Instant.parse("2026-10-16T10:45:00Z"));

        when(consultationRepository.findById(consultationId))
                .thenReturn(Optional.of(existingConsultation));
        when(consultationRepository.save(any(Consultation.class)))
                .thenAnswer(invocation -> {
                    Consultation saved = invocation.getArgument(0);
                    // For updates, the repository returns the saved object with the same ID and version
                    // (version would be incremented by JPA/Hibernate in real implementation)
                    return saved;
                });

        Consultation updateRequest = createTestConsultation(consultationId);
        updateRequest.setConsultationType("General");
        updateRequest.setUpdatedAt(Instant.parse("2026-10-16T11:00:00Z"));

        // When
        Consultation result = consultationService.updateConsultation(consultationId, updateRequest);

        // Then
        assertThat(result.getConsultationType()).isEqualTo("General");
        assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-16T11:00:00Z"));
        // CreatedAt should remain unchanged
        assertThat(result.getCreatedAt()).isEqualTo(existingConsultation.getCreatedAt());
        // ConsultationId and PatientId should remain unchanged (immutable)
        assertThat(result.getConsultationId()).isEqualTo(consultationId);
        assertThat(result.getPatientId()).isEqualTo(existingConsultation.getPatientId());
        // Version should remain the same (will be updated by JPA in infrastructure layer)
        assertThat(result.getVersion()).isEqualTo(existingConsultation.getVersion());

        InOrder inOrder = inOrder(consultationRepository);
        inOrder.verify(consultationRepository).findById(consultationId);
        inOrder.verify(consultationRepository).save(any(Consultation.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentConsultation() {
        // Given
        Long consultationId = 1L;
        when(consultationRepository.findById(consultationId))
                .thenReturn(Optional.empty());

        Consultation updateRequest = createTestConsultation(consultationId);

        // When/Then
        assertThatThrownBy(() -> consultationService.updateConsultation(consultationId, updateRequest))
                .isInstanceOf(ConsultationNotFoundException.class)
                .hasMessageContaining("Consultation not found for ID: " + consultationId);
        verify(consultationRepository).findById(consultationId);
        verify(consultationRepository, never()).save(any(Consultation.class));
    }


    private Consultation createTestConsultation(Long consultationId) {
        return new Consultation(
                consultationId,
                1L, // patientId
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
    }
}