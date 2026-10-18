package com.cecamed.backend.infrastructure.config;

import com.cecamed.backend.application.consultation.ConsultationRepositoryPort;
import com.cecamed.backend.application.consultation.service.ConsultationService;
import com.cecamed.backend.application.record.MedicalRecordRepositoryPort;
import com.cecamed.backend.application.record.service.MedicalRecordService;
import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ClinicalServiceConfiguration {
    @Bean
    MedicalRecordService medicalRecordService(MedicalRecordRepositoryPort repository, Clock clock) {
        return new MedicalRecordService(repository, clock);
    }
    @Bean
    ConsultationService consultationService(ConsultationRepositoryPort repository, PatientRepositoryPort patients, Clock clock) {
        return new ConsultationService(repository, patients, clock);
    }
}
