package com.cecamed.backend.application.scheduling;

import static com.cecamed.backend.application.scheduling.SchedulingDtos.*;

public interface SchedulingAdministrationUseCase {
  DoctorResponse registerDoctor(DoctorRequest request, String actor);

  void grantPatientAccess(long patientId, PatientAccessRequest request, String actor);
}
