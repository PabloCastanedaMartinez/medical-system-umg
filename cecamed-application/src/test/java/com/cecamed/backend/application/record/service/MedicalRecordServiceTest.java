package com.cecamed.backend.application.record.service;

import com.cecamed.backend.application.record.GetMedicalRecordUseCase;
import com.cecamed.backend.application.record.MedicalRecordDtos;
import com.cecamed.backend.application.record.UpdateMedicalRecordUseCase;
import com.cecamed.backend.application.record.service.MedicalRecordService;
import com.cecamed.backend.domain.record.MedicalRecord;
import com.cecamed.backend.domain.record.MedicalRecordNotFoundException;
import com.cecamed.backend.application.record.MedicalRecordRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the MedicalRecordService.
 */
class MedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepositoryPort medicalRecordRepository;

    private MedicalRecordService medicalRecordService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        medicalRecordService = new MedicalRecordService(medicalRecordRepository, java.time.Clock.fixed(Instant.parse("2026-10-16T11:00:00Z"), java.time.ZoneOffset.UTC));
    }

    @Test
    void shouldGetMedicalRecordWhenExists() {
        // Given
        Long patientId = 1L;
        MedicalRecord expectedRecord = createTestMedicalRecord(patientId);
        when(medicalRecordRepository.findByPatientId(patientId))
                .thenReturn(Optional.of(expectedRecord));

        // When
        MedicalRecord result = medicalRecordService.getMedicalRecord(patientId);

        // Then
        assertThat(result).isEqualTo(expectedRecord);
        verify(medicalRecordRepository).findByPatientId(patientId);
    }

    @Test
    void shouldThrowExceptionWhenMedicalRecordNotFound() {
        // Given
        Long patientId = 1L;
        when(medicalRecordRepository.findByPatientId(patientId))
                .thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> medicalRecordService.getMedicalRecord(patientId))
                .isInstanceOf(MedicalRecordNotFoundException.class)
                .hasMessageContaining("Medical record not found for patient with ID: " + patientId);
        verify(medicalRecordRepository).findByPatientId(patientId);
    }

    @Test
    void shouldUpdateMedicalRecordSuccessfully() {
        // Given
        Long patientId = 1L;
        MedicalRecord existingRecord = createTestMedicalRecord(patientId);
        existingRecord.setAllergies("Updated allergies");
        existingRecord.setUpdatedAt(Instant.parse("2026-10-16T10:45:00Z"));

        when(medicalRecordRepository.findByPatientId(patientId))
                .thenReturn(Optional.of(existingRecord));
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MedicalRecord updateRequest = createTestMedicalRecord(patientId);
        updateRequest.setAllergies("New allergies");
        updateRequest.setUpdatedAt(Instant.parse("2026-10-16T11:00:00Z"));

        // When
        MedicalRecord result = medicalRecordService.updateMedicalRecord(patientId, updateRequest);

        // Then
        assertThat(result.getAllergies()).isEqualTo("New allergies");
        assertThat(result.getUpdatedAt()).isEqualTo(Instant.parse("2026-10-16T11:00:00Z"));
        // CreatedAt should remain unchanged
        assertThat(result.getCreatedAt()).isEqualTo(existingRecord.getCreatedAt());
        // Version should remain the same (will be updated by JPA in infrastructure layer)
        assertThat(result.getVersion()).isEqualTo(existingRecord.getVersion());
        
        InOrder inOrder = inOrder(medicalRecordRepository);
        inOrder.verify(medicalRecordRepository).findByPatientId(patientId);
        inOrder.verify(medicalRecordRepository).save(any(MedicalRecord.class));
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistentMedicalRecord() {
        // Given
        Long patientId = 1L;
        when(medicalRecordRepository.findByPatientId(patientId))
                .thenReturn(Optional.empty());

        MedicalRecord updateRequest = createTestMedicalRecord(patientId);

        // When/Then
        assertThatThrownBy(() -> medicalRecordService.updateMedicalRecord(patientId, updateRequest))
                .isInstanceOf(MedicalRecordNotFoundException.class)
                .hasMessageContaining("Medical record not found for patient with ID: " + patientId);
        verify(medicalRecordRepository).findByPatientId(patientId);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    private MedicalRecord createTestMedicalRecord(Long patientId) {
        return new MedicalRecord(
                patientId,
                "MR000123",
                "Penicillin",
                "Hypertension",
                "None",
                "Diabetes in father",
                "Appendectomy",
                "None",
                "Lisinopril",
                "Patient in good health",
                "G2P2",
                8,
                3,
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z"),
                0L
        );
    }
}