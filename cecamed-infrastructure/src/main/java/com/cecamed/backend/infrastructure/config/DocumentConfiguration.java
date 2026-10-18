package com.cecamed.backend.infrastructure.config;

import com.cecamed.backend.application.document.*;
import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
public class DocumentConfiguration {
    @Bean
    DocumentUseCase documentUseCase(DocumentRepositoryPort repository, DocumentStoragePort storage,
            PatientRepositoryPort patients, Clock clock) {
        return new DocumentService(repository, storage, patients, clock);
    }
}
