package com.cecamed.backend.application.patient.model;

/** DTO inmutable; la edad se calcula con el reloj y la zona de la clínica. */
public record PatientResponse(
    Long id,
    String identificationNumber,
    String firstName,
    String lastName,
    String fullName,
    String birthDate,
    Integer age,
    String gender,
    String bloodType,
    String phone,
    String email,
    String address,
    String emergencyContactName,
    String emergencyContactPhone,
    String emergencyContactRelationship,
    String notes,
    Boolean active,
    String medicalRecordNumber,
    String createdAt,
    String updatedAt,
    Long version) {}
