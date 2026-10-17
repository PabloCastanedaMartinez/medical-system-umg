package com.cecamed.backend.infrastructure.config;

import com.cecamed.backend.application.patient.port.in.PatientManagementUseCase;
import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.application.identity.port.out.SecurityAuditRepositoryPort;
import com.cecamed.backend.application.patient.service.PatientService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de servicios de aplicación para pacientes.
 */
@Configuration(proxyBeanMethods = false)
public class PatientServiceConfiguration {

    @Bean
    public PatientManagementUseCase patientService(
            PatientRepositoryPort patientRepository,
            SecurityAuditRepositoryPort auditRepository,
            Clock clock) {
        return new PatientService(patientRepository, auditRepository, clock);
    }
}