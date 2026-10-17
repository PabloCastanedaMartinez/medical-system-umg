package com.cecamed.backend.application.patient.model;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;

/**
 * DTO para respuestas de pacientes.
 * Coincide con el Patient del frontend, incluyendo campos calculados como fullName y edad.
 */
public class PatientResponse {

    private Long id;
    private String identificationNumber; // puede ser null
    private String firstName;
    private String lastName;
    private String fullName;
    private String birthDate; // formato ISO-8601 (yyyy-MM-dd) o null
    private Integer age; // edad en años, puede ser null
    private String gender; // MASCULINO, FEMENINO o null
    private String bloodType; // según enum del frontend o null
    private String phone; // puede ser null
    private String email; // puede ser null
    private String address; // puede ser null
    private String emergencyContactName; // puede ser null
    private String emergencyContactPhone; // puede ser null
    private String emergencyContactRelationship; // puede ser null
    private String notes; // puede ser null
    private Boolean active; // puede ser null
    private String medicalRecordNumber; // puede ser null
    private String createdAt; // formato ISO-8601 con zona (Z) o null
    private String updatedAt; // formato ISO-8601 con zona (Z) o null

    // Constructors, Getters y Setters
    public PatientResponse() {
    }

    public PatientResponse(Long id, String identificationNumber, String firstName, String lastName,
                           String fullName, String birthDate, Integer age, String gender, String bloodType,
                           String phone, String email, String address, String emergencyContactName,
                           String emergencyContactPhone, String emergencyContactRelationship,
                           String notes, Boolean active, String medicalRecordNumber,
                           String createdAt, String updatedAt) {
        this.id = id;
        this.identificationNumber = identificationNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.birthDate = birthDate;
        this.age = age;
        this.gender = gender;
        this.bloodType = bloodType;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.notes = notes;
        this.active = active;
        this.medicalRecordNumber = medicalRecordNumber;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }

    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Calcula la edad en años a partir de la fecha de nacimiento y la fecha actual.
     * Este método es estático y puede ser usado por el servicio para establecer la edad.
     *
     * @param birthDateString fecha de nacimiento en formato ISO-8601 (yyyy-MM-dd)
     * @return edad en años o null si la fecha es null
     */
    public static Integer calculateAge(String birthDateString) {
        if (birthDateString == null || birthDateString.isEmpty()) {
            return null;
        }
        LocalDate birthDate = LocalDate.parse(birthDateString);
        LocalDate now = LocalDate.now();
        return Period.between(birthDate, now).getYears();
    }
}