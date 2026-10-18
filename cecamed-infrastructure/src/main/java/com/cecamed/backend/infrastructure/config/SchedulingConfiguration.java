package com.cecamed.backend.infrastructure.config;

import com.cecamed.backend.application.patient.port.out.PatientRepositoryPort;
import com.cecamed.backend.application.scheduling.*;
import java.time.Clock;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
public class SchedulingConfiguration {
  @Bean
  SchedulingService schedulingService(
      SchedulingRepositoryPort repository, PatientRepositoryPort patients, Clock clock) {
    return new SchedulingService(repository, patients, clock);
  }

  @Bean
  SchedulingAdministrationUseCase schedulingAdministration(
      SchedulingRepositoryPort repository,
      SchedulingUseCase scheduling,
      com.cecamed.backend.application.identity.port.in.UserManagementUseCase users,
      com.cecamed.backend.application.identity.port.out.UserRepositoryPort accounts,
      com.cecamed.backend.application.patient.port.in.PatientManagementUseCase patients) {
    return new SchedulingAdministrationService(repository, scheduling, users, accounts, patients);
  }
}
