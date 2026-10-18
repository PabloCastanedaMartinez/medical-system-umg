package com.cecamed.backend.api.clinical;

import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.domain.identity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import javax.sql.DataSource;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** El mismo contrato se ejecuta con H2 y con PostgreSQL/Flyway reales. */
abstract class ClinicalFlowContract {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserManagementUseCase users;
    @Autowired DataSource dataSource;
    String username;
    String token;
    String refreshToken;
    static final String PASSWORD = "Clinical-Test-482!";
    static final String PATIENT = """
        {"firstName":"Ana","lastName":"Prueba","birthDate":"2000-01-02",
         "gender":"FEMENINO","bloodType":"DESCONOCIDO","phone":"55551234","allergies":"Sin alergias conocidas"}
        """;
    static final String CONSULTATION = """
        {"consultationType":"GENERAL","doctorName":"Doctora Prueba","reasonForVisit":"Control",
         "diagnosis":"Saludable","icd10Code":"Z00","vitalSigns":"{\\\"heartRate\\\":70}"}
        """;

    @BeforeEach
    void login() throws Exception {
        username = "med" + UUID.randomUUID().toString().replace("-", "");
        users.createUser(new CreateUserCommand(username, PASSWORD, "Doctora Prueba", Role.MEDICO,
                "test", "127.0.0.1", "clinical-contract"));
        var result = call(post("/api/v1/auth/login"),
                json.writeValueAsString(java.util.Map.of("username", username, "password", PASSWORD)), 200);
        token = result.get("accessToken").asText();
        refreshToken = result.get("refreshToken").asText();
    }

    JsonNode call(MockHttpServletRequestBuilder request, String body, int status) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        request.header("User-Agent", "clinical-contract");
        if (body != null) request.contentType(MediaType.APPLICATION_JSON).content(body);
        var result = mvc.perform(request).andExpect(status().is(status)).andReturn();
        var content = result.getResponse().getContentAsString();
        return content.isBlank() ? null : json.readTree(content);
    }

    @Test
    void loginPatientRecordConsultationAndStaleEdits() throws Exception {
        var patient = call(post("/api/v1/patients"), PATIENT, 201);
        long id = patient.get("id").asLong();
        assertThat(patient.get("version").asLong()).isZero();
        assertThat(patient.get("createdAt").asText()).endsWith("Z");
        String patientPath = "/api/v1/patients/" + id;
        var record = call(get(patientPath + "/medical-record"), null, 200);
        assertThat(record.get("recordNumber").asText()).isEqualTo(patient.get("medicalRecordNumber").asText());
        var updatedRecord = call(put(patientPath + "/medical-record"), "{\"allergies\":\"Penicilina\",\"version\":0}", 200);
        assertThat(updatedRecord.get("version").asLong()).isEqualTo(1);
        call(put(patientPath + "/medical-record"), "{\"allergies\":\"Obsoleto\",\"version\":0}", 409);
        call(put(patientPath + "/medical-record"), "{\"allergies\":\"Sin versión\"}", 400);
        assertThat(call(get(patientPath + "/medical-record"), null, 200).get("allergies").asText()).isEqualTo("Penicilina");

        String update = PATIENT.trim().replace("\"Ana\"", "\"Ana Actualizada\"");
        update = update.substring(0, update.length()-1) + ",\"version\":0}";
        var updated = call(put(patientPath), update, 200);
        assertThat(updated.get("version").asLong()).isEqualTo(1);
        assertThat(updated.get("createdAt").asText()).isEqualTo(patient.get("createdAt").asText());
        call(put(patientPath), update, 409);
        call(put(patientPath), PATIENT, 400);

        var consultation = call(post(patientPath + "/consultations"), CONSULTATION, 201);
        long consultationId = consultation.get("consultationId").asLong();
        assertThat(consultationId).isPositive();
        assertThat(consultation.get("consultationDate").asText()).endsWith("Z");
        String consultationPath = patientPath + "/consultations/" + consultationId;
        String edit = CONSULTATION.trim();
        edit = edit.substring(0,edit.length()-1) + ",\"version\":0}";
        assertThat(call(put(consultationPath), edit, 200).get("version").asLong()).isEqualTo(1);
        call(put(consultationPath), edit, 409);
        call(put(consultationPath), CONSULTATION, 400);
        assertThat(call(get(consultationPath), null, 200).get("icd10Code").asText()).isEqualTo("Z00");
        var second = call(post(patientPath + "/consultations"), CONSULTATION, 201);
        assertThat(second.get("consultationId").asLong()).isNotEqualTo(consultationId);
        assertThat(call(get(patientPath + "/consultations"), null, 200).size()).isEqualTo(2);
        call(get("/api/v1/patients/999999999/consultations/" + consultationId), null, 404);
        call(post("/api/v1/patients/999999999/consultations"), CONSULTATION, 404);

        try (var connection = dataSource.getConnection(); var query = connection.prepareStatement(
                "SELECT username, ip_address, user_agent FROM cecamed.security_audit_logs WHERE event_type='PATIENT_CREATED' AND username=?")) {
            query.setString(1, username);
            try (var rows = query.executeQuery()) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getString(1)).isEqualTo(username);
                assertThat(rows.getString(2)).isEqualTo("127.0.0.1");
                assertThat(rows.getString(3)).isEqualTo("clinical-contract");
            }
        }
    }

    @Test
    void patientsValidateSearchAndPaginateWithoutLosingFilters() throws Exception {
        String marker = UUID.randomUUID().toString();
        String body = PATIENT.replace("Prueba", marker);
        var first = call(post("/api/v1/patients"), body, 201);
        call(post("/api/v1/patients"), body, 201);
        call(patch("/api/v1/patients/" + first.get("id").asLong() + "/deactivate").param("version", "0"), null, 204);
        var active = call(get("/api/v1/patients").param("search", marker).param("status", "ACTIVE").param("size", "1"), null, 200);
        assertThat(active.get("totalElements").asLong()).isEqualTo(1);
        assertThat(active.get("content").size()).isEqualTo(1);
        var all = call(get("/api/v1/patients").param("search", "Ana " + marker).param("size", "1").param("page", "1"), null, 200);
        assertThat(all.get("totalElements").asLong()).isEqualTo(2);
        assertThat(all.get("content").size()).isEqualTo(1);
        assertThat(call(get("/api/v1/patients").param("search", "%"), null, 200).get("totalElements").asLong()).isZero();
        call(post("/api/v1/patients"), PATIENT.replace("2000-01-02", "2099-01-01"), 400);
        call(post("/api/v1/patients"), PATIENT.replace("2000-01-02", "not-a-date"), 400);
        call(post("/api/v1/patients"), PATIENT.replace("FEMENINO", "INVALID"), 400);
        call(get("/api/v1/patients/999999999"), null, 404);
        call(post("/api/v1/patients/" + first.get("id").asLong() + "/consultations"), CONSULTATION, 409);
        call(patch("/api/v1/patients/" + first.get("id").asLong() + "/activate").param("version", "1"), null, 204);
    }

    @Test
    void simultaneousEditsHaveExactlyOneWinner() throws Exception {
        var patient = call(post("/api/v1/patients"), PATIENT, 201);
        String path = "/api/v1/patients/" + patient.get("id").asLong();
        var consultation = call(post(path + "/consultations"), CONSULTATION, 201);
        String[] paths = {path, path + "/medical-record", path + "/consultations/" + consultation.get("consultationId").asLong()};
        String[] bodies = {PATIENT.trim(), "{\"allergies\":\"Actualizado\"}", CONSULTATION.trim()};
        for (int i = 0; i < paths.length; i++) {
            final String endpoint = paths[i];
            final String body = bodies[i].substring(0, bodies[i].length()-1) + ",\"version\":0}";
            var start = new java.util.concurrent.CountDownLatch(1);
            try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
                java.util.concurrent.Callable<Integer> update = () -> {
                    start.await();
                    return mvc.perform(put(endpoint).header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus();
                };
                var first = pool.submit(update);
                var second = pool.submit(update);
                start.countDown();
                assertThat(java.util.List.of(first.get(), second.get())).containsExactlyInAnyOrder(200, 409);
            }
            assertThat(call(get(endpoint), null, 200).get("version").asLong()).isEqualTo(1);
        }
    }

    @Test
    void refreshRotatesAndLogoutRevokesTheActualToken() throws Exception {
        var refreshed = call(post("/api/v1/auth/refresh"), json.writeValueAsString(java.util.Map.of("refreshToken", refreshToken)), 200);
        assertThat(refreshed.get("refreshToken").asText()).isNotEqualTo(refreshToken);
        token = refreshed.get("accessToken").asText();
        call(get("/api/v1/auth/me"), null, 200);
        String body = json.writeValueAsString(java.util.Map.of("refreshToken", refreshed.get("refreshToken").asText()));
        call(post("/api/v1/auth/logout"), body, 204);
        call(post("/api/v1/auth/refresh"), body, 401);
    }

    @Test
    void receptionAndAdminCannotReadClinicalContent() throws Exception {
        var patient = call(post("/api/v1/patients"), PATIENT, 201);
        for (Role role : new Role[]{Role.RECEPCION, Role.ADMIN}) {
            var name = role.name() + UUID.randomUUID().toString().replace("-", "");
            users.createUser(new CreateUserCommand(name, PASSWORD, "Usuario", role, "test", "127.0.0.1", "test"));
            token = call(post("/api/v1/auth/login"), json.writeValueAsString(java.util.Map.of("username", name, "password", PASSWORD)), 200).get("accessToken").asText();
            call(get("/api/v1/patients/" + patient.get("id").asLong() + "/medical-record"), null, 403);
            call(get("/api/v1/patients/" + patient.get("id").asLong() + "/consultations"), null, 403);
            call(get("/api/v1/patients"), null, 200);
        }
    }
}
