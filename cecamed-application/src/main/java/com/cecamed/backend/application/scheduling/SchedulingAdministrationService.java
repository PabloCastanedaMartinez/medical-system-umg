package com.cecamed.backend.application.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

import com.cecamed.backend.application.identity.model.CreateUserCommand;
import com.cecamed.backend.application.identity.port.in.UserManagementUseCase;
import com.cecamed.backend.application.identity.port.out.UserRepositoryPort;
import com.cecamed.backend.application.patient.port.in.PatientManagementUseCase;
import com.cecamed.backend.domain.identity.Role;
import java.util.Locale;

/** Provisionar identidad y perfil es una sola transacción. */
public class SchedulingAdministrationService implements SchedulingAdministrationUseCase {
  private final SchedulingRepositoryPort repository;
  private final SchedulingUseCase scheduling;
  private final UserManagementUseCase users;
  private final UserRepositoryPort accounts;
  private final PatientManagementUseCase patients;

  public SchedulingAdministrationService(
      SchedulingRepositoryPort repository,
      SchedulingUseCase scheduling,
      UserManagementUseCase users,
      UserRepositoryPort accounts,
      PatientManagementUseCase patients) {
    this.repository = repository;
    this.scheduling = scheduling;
    this.users = users;
    this.accounts = accounts;
    this.patients = patients;
  }

  @Override
  public DoctorResponse registerDoctor(DoctorRequest request, String actor) {
    return repository.transaction(
        () -> {
          if (accounts
              .findByUsername(request.username().trim().toLowerCase(Locale.ROOT))
              .isEmpty()) {
            if (request.password() == null || request.password().isBlank())
              throw new IllegalArgumentException("Defina la contraseña de la nueva cuenta médica.");
            users.createUser(
                new CreateUserCommand(
                    request.username(),
                    request.password(),
                    request.fullName(),
                    Role.MEDICO,
                    actor,
                    null,
                    null));
          }
          return scheduling.saveDoctor(null, request);
        });
  }

  @Override
  public void grantPatientAccess(long patientId, PatientAccessRequest request, String actor) {
    repository.transaction(
        () -> {
          var patient = patients.getPatient(patientId);
          var user =
              users.createUser(
                  new CreateUserCommand(
                      request.username(),
                      request.password(),
                      patient.fullName(),
                      Role.PACIENTE,
                      actor,
                      null,
                      null));
          repository.linkPatient(patientId, user.id());
          return null;
        });
  }
}
