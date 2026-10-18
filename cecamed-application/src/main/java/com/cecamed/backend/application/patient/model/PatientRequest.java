package com.cecamed.backend.application.patient.model;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/** Alta y edición; las alergias iniciales se guardan en el expediente, sin duplicarlas. */
public record PatientRequest(
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotNull LocalDate birthDate,
    @NotBlank @Pattern(regexp = "MASCULINO|FEMENINO") String gender,
    @NotBlank @Pattern(regexp = "(A|B|AB|O)_(POSITIVO|NEGATIVO)|DESCONOCIDO") String bloodType,
    @Size(max = 25) String phone,
    @Size(max = 100) @Email String email,
    @Size(max = 255) String address,
    @Size(max = 100) String emergencyContactName,
    @Size(max = 25) String emergencyContactPhone,
    @Size(max = 50) String emergencyContactRelationship,
    @Size(max = 500) String notes,
    @PositiveOrZero Long version,
    @Size(max = 50) String identificationNumber,
    @NotBlank(groups = OnCreate.class) @Size(max = 2000) String allergies) {
  public interface OnCreate {}

  @AssertTrue(message = "Debe proporcionar teléfono o correo.")
  public boolean isContactProvided() {
    return (phone != null && !phone.isBlank()) || (email != null && !email.isBlank());
  }
}
