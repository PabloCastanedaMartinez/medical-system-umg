package com.cecamed.backend.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.TokenIssuerPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserAdminIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private PasswordEncoderPort passwordEncoder;
    @Autowired private TokenIssuerPort tokenIssuer;
    @Autowired private Clock clock;
    @Autowired private JsonMapper jsonMapper;

    private String adminToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        Instant now = clock.instant();

        UserAccount admin = userRepository.findByUsername("admin.user").orElseGet(() ->
                userRepository.save(UserAccount.createNew(
                        "admin.user",
                        passwordEncoder.encode("AdminPassword123!"),
                        "Admin User",
                        Role.ADMIN,
                        now
                ))
        );

        UserAccount doctor = userRepository.findByUsername("doc.user").orElseGet(() ->
                userRepository.save(UserAccount.createNew(
                        "doc.user",
                        passwordEncoder.encode("DoctorPass123!"),
                        "Doctor User",
                        Role.MEDICO,
                        now
                ))
        );

        adminToken = tokenIssuer.issueAccessToken(admin, now);
        doctorToken = tokenIssuer.issueAccessToken(doctor, now);
    }

    @Test
    void adminCanCreateListAndManageUsers() throws Exception {
        String createJson = """
                {
                    "username": "recep.new",
                    "password": "PasswordForRecep123!",
                    "fullName": "Recepcionista Nueva",
                    "role": "RECEPCION"
                }
                """;

        // Crear usuario (201 Created)
        var createResult = mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.username").value("recep.new"))
                .andExpect(jsonPath("$.role").value("RECEPCION"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        long newUserId = jsonMapper.readTree(responseBody).get("id").asLong();

        // Listar usuarios (200 OK)
        mvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());

        // Consultar por ID (200 OK)
        mvc.perform(get("/api/v1/users/" + newUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("recep.new"));

        // Actualizar datos y rol (200 OK)
        String updateJson = """
                {
                    "fullName": "Recepcionista Promovida",
                    "role": "MEDICO"
                }
                """;
        mvc.perform(put("/api/v1/users/" + newUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Recepcionista Promovida"))
                .andExpect(jsonPath("$.role").value("MEDICO"));

        // Desactivar usuario (200 OK)
        mvc.perform(patch("/api/v1/users/" + newUserId + "/deactivate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        // Activar usuario (200 OK)
        mvc.perform(patch("/api/v1/users/" + newUserId + "/activate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        // Restablecer contraseña (204 No Content)
        String resetJson = "{\"newPassword\":\"BrandNewPassword123!\"}";
        mvc.perform(post("/api/v1/users/" + newUserId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(resetJson))
                .andExpect(status().isNoContent());
    }

    @Test
    void nonAdminCannotAccessUserManagementEndpoints() throws Exception {
        mvc.perform(get("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctorToken))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));

        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void anonymousCannotAccessUserManagementEndpoints() throws Exception {
        mvc.perform(get("/api/v1/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void validationErrorsReturn400ProblemDetails() throws Exception {
        String invalidJson = """
                {
                    "username": "",
                    "password": "short",
                    "fullName": "",
                    "role": null
                }
                """;

        mvc.perform(post("/api/v1/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray());
    }
}
