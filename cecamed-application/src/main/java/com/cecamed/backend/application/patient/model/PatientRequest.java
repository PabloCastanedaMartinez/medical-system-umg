package com.cecamed.backend.application.patient.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * DTO para solicitudes de creación y actualización de pacientes.
 * Coincide con el PatientRequest del frontend.
 */
public class PatientRequest {

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    private String lastName;

    @NotNull
    private java.time.LocalDate birthDate; // formato ISO-8601 (yyyy-MM-dd)

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "MASCULINO|FEMENINO")
    private String gender; // valores esperados: MASCULINO, FEMENINO

    @NotBlank
    @jakarta.validation.constraints.Pattern(regexp = "(A|B|AB|O)_(POSITIVO|NEGATIVO)|DESCONOCIDO")
    private String bloodType; // valores esperados según enum del frontend

    @Size(max = 25)
    private String phone;

    @Size(max = 100)
    @jakarta.validation.constraints.Email
    private String email;

    @Size(max = 255)
    private String address;

    @Size(max = 100)
    private String emergencyContactName;

    @Size(max = 25)
    private String emergencyContactPhone;

    @Size(max = 50)
    private String emergencyContactRelationship;

    @Size(max = 500)
    private String notes;

    private Long version;
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    @Size(max = 50)
    private String identificationNumber;
    public String getIdentificationNumber() { return identificationNumber; }
    public void setIdentificationNumber(String value) { identificationNumber = value; }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public java.time.LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(java.time.LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodType() {
        return bloodType;
    }

    public void setBloodType(String bloodType) {
        this.bloodType = bloodType;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public void setEmergencyContactPhone(String emergencyContactPhone) {
        this.emergencyContactPhone = emergencyContactPhone;
    }

    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    public void setEmergencyContactRelationship(String emergencyContactRelationship) {
        this.emergencyContactRelationship = emergencyContactRelationship;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}