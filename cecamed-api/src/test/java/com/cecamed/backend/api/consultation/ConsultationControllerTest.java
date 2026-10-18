package com.cecamed.backend.api.consultation;

import com.cecamed.backend.application.consultation.CreateConsultationUseCase;
import com.cecamed.backend.application.consultation.GetConsultationByIdUseCase;
import com.cecamed.backend.application.consultation.GetConsultationsByPatientIdUseCase;
import com.cecamed.backend.application.consultation.UpdateConsultationUseCase;
import com.cecamed.backend.application.consultation.ConsultationDtos;
import com.cecamed.backend.api.error.ApiExceptionHandler;
import com.cecamed.backend.domain.consultation.Consultation;
import com.cecamed.backend.domain.consultation.ConsultationNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for the ConsultationController.
 */
class ConsultationControllerTest {

    private MockMvc mockMvc;

    private GetConsultationByIdUseCase getConsultationByIdUseCase;
    private GetConsultationsByPatientIdUseCase getConsultationsByPatientIdUseCase;
    private CreateConsultationUseCase createConsultationUseCase;
    private UpdateConsultationUseCase updateConsultationUseCase;
    private ObjectMapper objectMapper;

    private Consultation testConsultation;
    private ConsultationDtos.Response testResponse;
    private List<Consultation> testConsultationsList;
    private List<ConsultationDtos.Response> testConsultationsResponsesList;

    @BeforeEach
    void setUp() {
        getConsultationByIdUseCase = mock(GetConsultationByIdUseCase.class);
        getConsultationsByPatientIdUseCase = mock(GetConsultationsByPatientIdUseCase.class);
        createConsultationUseCase = mock(CreateConsultationUseCase.class);
        updateConsultationUseCase = mock(UpdateConsultationUseCase.class);
        objectMapper = new ObjectMapper();

        ConsultationController controller = new ConsultationController(
                getConsultationByIdUseCase,
                getConsultationsByPatientIdUseCase,
                createConsultationUseCase,
                updateConsultationUseCase
        );

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        testConsultation = new Consultation(
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

        testResponse = new ConsultationDtos.Response(
                testConsultation.getConsultationId(),
                testConsultation.getPatientId(),
                testConsultation.getConsultationType(),
                testConsultation.getDoctorName(),
                testConsultation.getReasonForVisit(),
                testConsultation.getSymptoms(),
                testConsultation.getExaminationFindings(),
                testConsultation.getDiagnosis(),
                testConsultation.getTreatmentPlan(),
                testConsultation.getPrescriptions(),
                testConsultation.getFollowUpInstructions(),
                testConsultation.getVitalSigns(),
                testConsultation.getNotes(),
                testConsultation.getCreatedAt().toString(),
                testConsultation.getUpdatedAt().toString()
        );

        // Create a second consultation for list testing
        Consultation testConsultation2 = new Consultation(
                2L,
                1L,
                Instant.parse("2026-09-15T14:00:00Z"),
                "Follow-up",
                "Dr. Jones",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication",
                Instant.parse("2026-09-15T14:00:00Z"),
                Instant.parse("2026-09-15T14:00:00Z"),
                0L
        );

        testConsultationsList = Arrays.asList(testConsultation2, testConsultation); // Most recent first
        testConsultationsResponsesList = testConsultationsList.stream()
                .map(this::toResponse)
                .toList();
    }

    private ConsultationDtos.Response toResponse(Consultation consultation) {
        return new ConsultationDtos.Response(
                consultation.getConsultationId(),
                consultation.getPatientId(),
                consultation.getConsultationType(),
                consultation.getDoctorName(),
                consultation.getReasonForVisit(),
                consultation.getSymptoms(),
                consultation.getExaminationFindings(),
                consultation.getDiagnosis(),
                consultation.getTreatmentPlan(),
                consultation.getPrescriptions(),
                consultation.getFollowUpInstructions(),
                consultation.getVitalSigns(),
                consultation.getNotes(),
                consultation.getCreatedAt().toString(),
                consultation.getUpdatedAt().toString()
        );
    }

    @Test
    void shouldGetConsultationsWhenExist() throws Exception {
        // Given
        Long patientId = 1L;
        BDDMockito.given(getConsultationsByPatientIdUseCase.getConsultationsByPatientId(patientId))
                .willReturn(testConsultationsList);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/consultations", patientId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].consultationId").value(testConsultationsList.get(0).getConsultationId()))
                .andExpect(jsonPath("$[0].reasonForVisit").value(testConsultationsList.get(0).getReasonForVisit()))
                .andExpect(jsonPath("$[1].consultationId").value(testConsultationsList.get(1).getConsultationId()))
                .andExpect(jsonPath("$[1].reasonForVisit").value(testConsultationsList.get(1).getReasonForVisit()));
    }

    @Test
    void shouldReturn404WhenConsultationNotFoundById() throws Exception {
        // Given
        Long consultationId = 999L;
        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willThrow(new ConsultationNotFoundException(consultationId));

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/consultations/{consultationId}", 1L, consultationId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldGetConsultationWhenExistsById() throws Exception {
        // Given
        Long patientId = 1L;
        Long consultationId = 1L;
        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willReturn(testConsultation);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/consultations/{consultationId}", patientId, consultationId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.consultationId").value(testConsultation.getConsultationId()))
                .andExpect(jsonPath("$.reasonForVisit").value(testConsultation.getReasonForVisit()))
                .andExpect(jsonPath("$.doctorName").value(testConsultation.getDoctorName()));
    }

    @Test
    void shouldReturn404WhenConsultationDoesNotBelongToPatient() throws Exception {
        // Given
        Long patientId = 1L;
        Long consultationId = 1L;
        Consultation consultationForOtherPatient = new Consultation(
                consultationId,
                999L, // Different patient ID
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

        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willReturn(consultationForOtherPatient);

        // When & Then
        mockMvc.perform(get("/api/v1/patients/{patientId}/consultations/{consultationId}", patientId, consultationId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateConsultationWhenValidRequest() throws Exception {
        // Given
        Long patientId = 1L;
        ConsultationDtos.Request createRequest = new ConsultationDtos.Request(
                "Follow-up",
                "Dr. Smith",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication"
        );

        Consultation createdConsultation = new Consultation(
                2L,
                patientId,
                Instant.parse("2026-10-16T10:00:00Z"),
                "Follow-up",
                "Dr. Smith",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication",
                Instant.parse("2026-10-16T10:00:00Z"),
                Instant.parse("2026-10-16T10:00:00Z"),
                0L
        );

        BDDMockito.given(createConsultationUseCase.createConsultation(
                ArgumentMatchers.argThat(consultation ->
                        consultation.getPatientId().equals(patientId) &&
                        consultation.getConsultationType().equals("Follow-up") &&
                        consultation.getDoctorName().equals("Dr. Smith")
                ))
        ).willReturn(createdConsultation);

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/consultations", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.consultationId").value(2L))
                .andExpect(jsonPath("$.consultationType").value("Follow-up"))
                .andExpect(jsonPath("$.reasonForVisit").value("Blood pressure check"));
    }

    @Test
    void shouldReturn400WhenCreateRequestIsInvalid() throws Exception {
        // Given
        Long patientId = 1L;
        ConsultationDtos.Request invalidRequest = new ConsultationDtos.Request(
                null, // Null consultationType to trigger @NotBlank validation
                "Dr. Smith",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication"
        );

        // Need to mock getConsultationsByPatientIdUseCase to return an existing list so the controller gets past validation
        // Actually, for POST we don't need to consult existing data first, but let's mock it anyway to be safe
        BDDMockito.given(getConsultationsByPatientIdUseCase.getConsultationsByPatientId(patientId))
                .willReturn(Arrays.asList(testConsultation));

        // When & Then
        mockMvc.perform(post("/api/v1/patients/{patientId}/consultations", patientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldUpdateConsultationWhenValidRequest() throws Exception {
        // Given
        Long patientId = 1L;
        Long consultationId = 1L;
        ConsultationDtos.Request updateRequest = new ConsultationDtos.Request(
                "Follow-up",
                "Dr. Smith",
                "Blood pressure recheck",
                "BP improved",
                "BP 120/80",
                "Hypertension",
                "Continue medication",
                "Medication X",
                "Check BP weekly",
                "BP 120/80",
                "Patient doing well"
        );

        Consultation existingConsultation = new Consultation(
                consultationId,
                patientId,
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

        Consultation updatedConsultation = new Consultation(
                consultationId,
                patientId,
                Instant.parse("2026-10-16T11:00:00Z"),
                "Follow-up",
                "Dr. Smith",
                "Blood pressure recheck",
                "BP improved",
                "BP 120/80",
                "Hypertension",
                "Continue medication",
                "Medication X",
                "Check BP weekly",
                "BP 120/80",
                "Patient doing well",
                Instant.parse("2026-10-15T09:30:00Z"),
                Instant.parse("2026-10-16T11:00:00Z"),
                0L
        );

        // Mock the getConsultationByIdUseCase to return existing consultation
        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willReturn(existingConsultation);

        // Mock the updateConsultationUseCase to return updated consultation when called with correct parameters
        BDDMockito.given(updateConsultationUseCase.updateConsultation(
                ArgumentMatchers.eq(consultationId),
                ArgumentMatchers.argThat(consultation ->
                        consultation.getConsultationId().equals(consultationId) &&
                        consultation.getReasonForVisit().equals("Blood pressure recheck") &&
                        consultation.getSymptoms().equals("BP improved")
                ))
        ).willReturn(updatedConsultation);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/consultations/{consultationId}", patientId, consultationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.reasonForVisit").value("Blood pressure recheck"))
                .andExpect(jsonPath("$.symptoms").value("BP improved"))
                .andExpect(jsonPath("$.consultationType").value("Follow-up"));
    }

    @Test
    void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception {
        // Given
        Long patientId = 1L;
        Long consultationId = 1L;
        ConsultationDtos.Request invalidRequest = new ConsultationDtos.Request(
                null, // Null consultationType to trigger @NotBlank validation
                "Dr. Smith",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication"
        );

        // Need to mock getConsultationByIdUseCase to return an existing consultation so the controller gets past the first step
        Consultation existingConsultation = new Consultation(
                consultationId,
                patientId,
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

        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willReturn(existingConsultation);

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/consultations/{consultationId}", patientId, consultationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404WhenUpdatingNonExistentConsultation() throws Exception {
        // Given
        Long patientId = 1L;
        Long consultationId = 999L;
        ConsultationDtos.Request updateRequest = new ConsultationDtos.Request(
                "Follow-up",
                "Dr. Smith",
                "Blood pressure check",
                "Slightly elevated BP",
                "BP 130/85",
                "Hypertension",
                "Increase medication",
                "Medication X",
                "Check BP weekly",
                "BP 130/85",
                "Patient compliant with medication"
        );

        // Mock the getConsultationByIdUseCase to throw exception when consultation not found
        BDDMockito.given(getConsultationByIdUseCase.getConsultationById(consultationId))
                .willThrow(new ConsultationNotFoundException(consultationId));

        // When & Then
        mockMvc.perform(put("/api/v1/patients/{patientId}/consultations/{consultationId}", patientId, consultationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }
}