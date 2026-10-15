package com.cecamed.backend.api.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.cecamed.backend.application.shared.ResourceNotFoundException;
import com.cecamed.backend.domain.shared.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

class ApiExceptionHandlerTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void mapsMissingResourcesTo404() throws Exception {
        mvc.perform(get("/probe/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.instance").value("/probe/missing"))
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void preservesStableBusinessErrorCode() throws Exception {
        mvc.perform(get("/probe/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RULE_CONFLICT"));
    }

    @Test
    void returnsFieldErrorsWithoutRejectedValues() throws Exception {
        mvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    @Test
    void sanitizesUnreadableJson() throws Exception {
        mvc.perform(post("/probe/validation")
                        .contentType(MediaType.APPLICATION_JSON).content("{invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HTTP_400"))
                .andExpect(jsonPath("$.detail").value("No se pudo procesar la solicitud."));
    }

    @Test
    void preservesFrameworkMethodNotAllowedStatus() throws Exception {
        mvc.perform(post("/probe/missing"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("HTTP_405"));
    }

    @Test
    void hidesInternalExceptionDetails() throws Exception {
        mvc.perform(get("/probe/failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("Ocurrió un error interno."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @RestController
    static class ProbeController {

        @GetMapping("/probe/missing")
        String missing() {
            throw new ResourceNotFoundException("El recurso no existe.");
        }

        @GetMapping("/probe/conflict")
        String conflict() {
            throw new BusinessRuleException("RULE_CONFLICT", "La operación entra en conflicto.");
        }

        @GetMapping("/probe/failure")
        String failure() {
            throw new IllegalStateException("internal-secret-test-marker");
        }

        @PostMapping("/probe/validation")
        void validate(@Valid @RequestBody ProbeRequest request) {}
    }

    record ProbeRequest(@NotBlank String name) {}
}
