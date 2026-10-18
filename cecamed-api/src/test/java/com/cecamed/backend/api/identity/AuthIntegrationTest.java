package com.cecamed.backend.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cecamed.backend.application.identity.port.out.PasswordEncoderPort;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.domain.identity.Role;
import com.cecamed.backend.domain.identity.UserAccount;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepositoryPort userRepository;
    @Autowired private PasswordEncoderPort passwordEncoder;
    @Autowired private Clock clock;
    @Autowired private ObjectMapper jsonMapper;

    @BeforeEach
    void setUp() {
        Instant now = clock.instant();
        if (!userRepository.existsByUsername("dr.test")) {
            userRepository.save(UserAccount.createNew(
                    "dr.test",
                    passwordEncoder.encode("TestPassword123!"),
                    "Doctor Test",
                    Role.MEDICO,
                    now
            ));
        }
    }

    @Test
    void loginWithValidCredentialsReturnsTokensAndSessionInfo() throws Exception {
        String loginJson = """
                {
                    "username": "dr.test",
                    "password": "TestPassword123!"
                }
                """;

        var result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.username").value("dr.test"))
                .andExpect(jsonPath("$.fullName").value("Doctor Test"))
                .andExpect(jsonPath("$.role").value("MEDICO"))
                .andExpect(jsonPath("$.roleDisplayName").value("Médico"))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        var root = jsonMapper.readTree(responseBody);
        String accessToken = root.get("accessToken").asText();

        // Validar que con el accessToken podemos acceder a /api/v1/auth/me
        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("dr.test"))
                .andExpect(jsonPath("$.fullName").value("Doctor Test"))
                .andExpect(jsonPath("$.role").value("MEDICO"));
    }

    @Test
    void meRejectsAnonymousRequestWithProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    void refreshRotatesRefreshTokenAndDetectsReuse() throws Exception {
        String loginJson = """
                {
                    "username": "dr.test",
                    "password": "TestPassword123!"
                }
                """;

        String loginResponse = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String oldRefreshToken = jsonMapper.readTree(loginResponse).get("refreshToken").asText();

        // Refresco exitoso
        String refreshJson = "{\"refreshToken\":\"" + oldRefreshToken + "\"}";
        String refreshResponse = mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn().getResponse().getContentAsString();

        String newRefreshToken = jsonMapper.readTree(refreshResponse).get("refreshToken").asText();
        assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);

        // Intento de reusar el viejo refresh token: debe ser rechazado con 401 INVALID_TOKEN
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("reutilización indebida")));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        String loginJson = """
                {
                    "username": "dr.test",
                    "password": "TestPassword123!"
                }
                """;

        String loginResponse = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String refreshToken = jsonMapper.readTree(loginResponse).get("refreshToken").asText();

        // Logout
        mvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        // Intentar refrescar con el token revocado debe fallar
        mvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void locksAccountAfterConsecutiveFailedAttempts() throws Exception {
        String uniqueUser = "brute.force.test";
        userRepository.save(UserAccount.createNew(
                uniqueUser,
                passwordEncoder.encode("ValidPass1234!"),
                "Brute Force User",
                Role.RECEPCION,
                clock.instant()
        ));

        String badLoginJson = "{\"username\":\"" + uniqueUser + "\",\"password\":\"WrongPass!\"}";

        // 4 intentos fallidos: 401 INVALID_CREDENTIALS
        for (int i = 1; i <= 4; i++) {
            mvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(badLoginJson))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
        }

        // El 5to intento fallido supera el umbral y bloquea la cuenta
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));

        // Incluso con la contraseña correcta, el intento siguiente debe ser rechazado por cuenta bloqueada
        String correctLoginJson = "{\"username\":\"" + uniqueUser + "\",\"password\":\"ValidPass1234!\"}";
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(correctLoginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void rejectsTamperedTokenWithProblemDetails() throws Exception {
        mvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
    }

    @Test
    void systemInfoRemainsPublic() throws Exception {
        mvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("cecamed-backend"));
    }
}
