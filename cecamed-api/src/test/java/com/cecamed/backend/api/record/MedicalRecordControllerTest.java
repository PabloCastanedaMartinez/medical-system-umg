package com.cecamed.backend.api.record;

import com.cecamed.backend.application.record.GetMedicalRecordUseCase;
import com.cecamed.backend.application.record.MedicalRecordDtos;
import com.cecamed.backend.application.record.UpdateMedicalRecordUseCase;
import com.cecamed.backend.api.error.ApiExceptionHandler;
import com.cecamed.backend.domain.record.MedicalRecord;
import com.cecamed.backend.domain.record.MedicalRecordNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the MedicalRecordController.
 */
class MedicalRecordControllerTest {

    private MockMvc mockMvc;

    private GetMedicalRecordUseCase getMedicalRecordUseCase;
    private UpdateMedicalRecordUseCase updateMedicalRecordUseCase;
    private ObjectMapper objectMapper;

    private MedicalRecord testMedicalRecord;
    private MedicalRecordDtos.Response testResponse;

    @BeforeEach
    void setUp() {
        getMedicalRecordUseCase = mock(GetMedicalRecordUseCase.class);
        updateMedicalRecordUseCase = mock(UpdateMedicalRecordUseCase.class);
        objectMapper = new ObjectMapper();

        MedicalRecordController controller = new MedicalRecordController(
                getMedicalRecordUseCase,
                updateMedicalRecordUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        testMedicalRecord = new MedicalRecord(
                1L,
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
                Instant.parse("2026-10-15T09:30:00Z")
        );

        testResponse = new MedicalRecordDtos.Response(
                testMedicalRecord.getPatientId(),
                testMedicalRecord.getRecordNumber(),
                testMedicalRecord.getAllergies(),
                testMedicalRecord.getPathologicalHistory(),
                testMedicalRecord.getNonPathologicalHistory(),
                testMedicalRecord.getFamilyHistory(),
                testMedicalRecord.getSurgicalHistory(),
                testMedicalRecord.getTraumaticHistory(),
                testMedicalRecord.getCurrentMedications(),
                testMedicalRecord.getGeneralObservations(),
                testMedicalRecord.getGynecologicalObstetricHistory(),
                testMedicalRecord.getWaterGlassesPerDay(),
                testMedicalRecord.getMealsPerDay(),
                testMedicalRecord.getCreatedAt().toString(),
                testMedicalRecord.getUpdatedAt().toString(),
                0L);
    }

    @Test
    void shouldGetMedicalRecordWhenExists() throws Exception {
        // Given
        Long patientId = 1L;
        BDDMockito.given(getMedicalRecordUseCase.getMedicalRecord(patientId))
                .willReturn(testMedicalRecord);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/medical-record", patientId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.patientId").value(testMedicalRecord.getPatientId()))
                .andExpect(jsonPath("$.recordNumber").value(testMedicalRecord.getRecordNumber()))
                .andExpect(jsonPath("$.allergies").value(testMedicalRecord.getAllergies()))
                .andExpect(jsonPath("$.pathologicalHistory").value(testMedicalRecord.getPathologicalHistory()));
    }

    @Test
    void shouldReturn404WhenMedicalRecordNotFound() throws Exception {
        // Given
        Long patientId = 999L;
        BDDMockito.given(getMedicalRecordUseCase.getMedicalRecord(patientId))
                .willThrow(new MedicalRecordNotFoundException(patientId));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/medical-record", patientId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldUpdateMedicalRecordWhenValidRequest() throws Exception {
        // Given
        Long patientId = 1L;
        MedicalRecordDtos.Request updateRequest = new MedicalRecordDtos.Request(
                "New allergies",
                "New pathological history",
                "New non-pathological history",
                "New family history",
                "New surgical history",
                "New traumatic history",
                "New medications",
                "New general observations",
                "New gynecological history",
                10,
                4,
                0L);

        MedicalRecord existingRecord = new MedicalRecord(
                patientId,
                "MR000123",
                "Old allergies",
                "Old pathological history",
                "Old non-pathological history",
                "Old family history",
                "Old surgical history",
                "Old traumatic history",
                "Old medications",
                "Old general observations",
                "Old gynecological history",
                5,
                2,
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z")
        );

        MedicalRecord updatedRecord = new MedicalRecord(
                patientId,
                "MR000123",
                "New allergies",
                "New pathological history",
                "New non-pathological history",
                "New family history",
                "New surgical history",
                "New traumatic history",
                "New medications",
                "New general observations",
                "New gynecological history",
                10,
                4,
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-16T10:45:00Z")
        );

        // Mock the getMedicalRecordUseCase to return existing record
        BDDMockito.given(getMedicalRecordUseCase.getMedicalRecord(patientId))
                .willReturn(existingRecord);

        // Mock the updateMedicalRecordUseCase to return updated record when called with correct parameters
        BDDMockito.given(updateMedicalRecordUseCase.updateMedicalRecord(
                ArgumentMatchers.eq(patientId),
                ArgumentMatchers.argThat(record -> 
                        "New allergies".equals(record.getAllergies()) &&
                        "New pathological history".equals(record.getPathologicalHistory()) &&
                        record.getPatientId().equals(patientId)
                ))
        ).willReturn(updatedRecord);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/medical-record", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.allergies").value("New allergies"))
                .andExpect(jsonPath("$.generalObservations").value("New general observations"));
    }

    @Test
    void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception {
        // Given
        Long patientId = 1L;
        MedicalRecordDtos.Request invalidRequest = new MedicalRecordDtos.Request(
                "x".repeat(2001), // Exceeds the clinical field limit
                "New pathological history",
                "New non-pathological history",
                "New family history",
                "New surgical history",
                "New traumatic history",
                "New medications",
                "New general observations",
                "New gynecological history",
                10,
                4,
                0L);

        // Need to mock getMedicalRecordUseCase to return an existing record so the controller gets past the first step
        MedicalRecord existingRecord = new MedicalRecord(
                patientId,
                "MR000123",
                "Old allergies",
                "Old pathological history",
                "Old non-pathological history",
                "Old family history",
                "Old surgical history",
                "Old traumatic history",
                "Old medications",
                "Old general observations",
                "Old gynecological history",
                5,
                2,
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-15T09:30:00Z")
        );

        BDDMockito.given(getMedicalRecordUseCase.getMedicalRecord(patientId))
                .willReturn(existingRecord);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/medical-record", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentMedicalRecord() throws Exception {
        // Given
        Long patientId = 999L;
        MedicalRecordDtos.Request updateRequest = new MedicalRecordDtos.Request(
                "New allergies",
                "New pathological history",
                "New non-pathological history",
                "New family history",
                "New surgical history",
                "New traumatic history",
                "New medications",
                "New general observations",
                "New gynecological history",
                10,
                4,
                0L);

        // Mock the getMedicalRecordUseCase to throw exception when record not found
        BDDMockito.given(getMedicalRecordUseCase.getMedicalRecord(patientId))
                .willThrow(new MedicalRecordNotFoundException(patientId));

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/medical-record", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}