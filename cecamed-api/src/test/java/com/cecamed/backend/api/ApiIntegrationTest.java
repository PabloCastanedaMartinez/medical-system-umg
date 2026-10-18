package com.cecamed.backend.api;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Test
    void exposesVersionedSystemInformation() throws Exception {
        mvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cecamed-backend"))
                .andExpect(jsonPath("$.apiVersion").value("v1"))
                .andExpect(jsonPath("$.version", not(emptyOrNullString())));
    }

    @Test
    void exposesHealthWithoutDatabaseDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void generatesOpenApiContract() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("CECAMed Backend API"))
                .andExpect(jsonPath("$.paths['/api/v1/system/info'].get").exists());
    }

    @Test
    void deniesFutureBusinessRoutesForAnonymousUsers() throws Exception {
        mvc.perform(get("/api/v1/patients"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"))
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));
    }

    @Test
    void deniesFutureBusinessRoutesEvenWithAnAuthenticatedPrincipal() throws Exception {
        mvc.perform(get("/api/v1/patients").with(user("test-user")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void medicoCanReachClinicalControllers() throws Exception {
        mvc.perform(get("/api/v1/patients/999999/medical-record")
                        .with(user("doctor").roles("MEDICO")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEDICAL_RECORD_NOT_FOUND"));
        mvc.perform(get("/api/v1/patients/999999/consultations")
                        .with(user("doctor").roles("MEDICO")))
                .andExpect(status().isNotFound());
    }

    @Test
    void receptionAndAdminCannotReadClinicalData() throws Exception {
        for (String role : new String[]{"RECEPCION", "ADMIN"}) {
            for (String resource : new String[]{"medical-record", "consultations", "consultations/1", "documents", "documents/1/content"}) {
                mvc.perform(get("/api/v1/patients/1/" + resource)
                                .with(user("restricted").roles(role)))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
            }
        }
    }

    @Test
    void acceptsPreflightFromConfiguredFrontend() throws Exception {
        mvc.perform(options("/api/v1/patients")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
    }

    @Test
    void rejectsPreflightFromUnknownOrigin() throws Exception {
        mvc.perform(options("/api/v1/patients")
                        .header(HttpHeaders.ORIGIN, "https://unknown.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
