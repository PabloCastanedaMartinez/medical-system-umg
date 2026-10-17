package com.cecamed.backend.infrastructure.persistence.patient;

import com.cecamed.backend.domain.patient.Patient;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad JPA que representa a un paciente en la base de datos.
 */
@Entity
@Table(name = "patients", schema = "cecamed")
public class PatientJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identification_number", length = 50, unique = true)
    private String identificationNumber;

    @Column(name = "first_name", nullable = false, length = 150)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 150)
    private String lastName;

    @Column(name = "full_name", nullable = false, length = 300)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @Column(name = "blood_type", nullable = false, length = 20)
    private String bloodType;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 50)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 50)
    private String emergencyContactRelationship;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "medical_record_number", length = 50)
    private String medicalRecordNumber;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PatientJpaEntity() {}

    public PatientJpaEntity(
            Long id,
            String identificationNumber,
            String firstName,
            String lastName,
            String fullName,
            LocalDate birthDate,
            String gender,
            String bloodType,
            String phone,
            String email,
            String address,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String notes,
            boolean active,
            String medicalRecordNumber) {
        this.id = id;
        this.identificationNumber = identificationNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.fullName = fullName;
        this.birthDate = birthDate;
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
        // createdAt and updatedAt will be set by @PrePersist and @PreUpdate
    }

    /**
     * Crea una instancia de PatientJpaEntity a partir del objeto de dominio.
     * Asume que el objeto de dominio ya ha sido validado y normalizado.
     *
     * @param domain objeto de dominio Patient
     * @return entidad JPA
     */
    public static PatientJpaEntity fromDomain(Patient domain) {
        return new PatientJpaEntity(
                domain.getId(),
                domain.getIdentificationNumber(),
                domain.getFirstName(),
                domain.getLastName(),
                domain.getFullName(),
                domain.getBirthDate(),
                domain.getGender(),
                domain.getBloodType(),
                domain.getPhone(),
                domain.getEmail(),
                domain.getAddress(),
                domain.getEmergencyContactName(),
                domain.getEmergencyContactPhone(),
                domain.getEmergencyContactRelationship(),
                domain.getNotes(),
                domain.isActive(),
                domain.getMedicalRecordNumber()
        );
    }

    /**
     * Convierte esta entidad JPA a un objeto de dominio Patient.
     * Nota: el objeto de dominio no incluye los campos de auditoría ni versión.
     *
     * @return objeto de dominio Patient
     */
    public Patient toDomain() {
        Patient patient = new Patient();
        patient.setId(this.id);
        patient.setIdentificationNumber(this.identificationNumber);
        patient.setFirstName(this.firstName);
        patient.setLastName(this.lastName);
        patient.setFullName(this.fullName);
        patient.setBirthDate(this.birthDate);
        patient.setGender(this.gender);
        patient.setBloodType(this.bloodType);
        patient.setPhone(this.phone);
        patient.setEmail(this.email);
        patient.setAddress(this.address);
        patient.setEmergencyContactName(this.emergencyContactName);
        patient.setEmergencyContactPhone(this.emergencyContactPhone);
        patient.setEmergencyContactRelationship(this.emergencyContactRelationship);
        patient.setNotes(this.notes);
        patient.setActive(this.active);
        patient.setMedicalRecordNumber(this.medicalRecordNumber);
        // Los campos de auditoría y versión no se mapean al dominio
        return patient;
    }

    @PrePersist
    void prePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters y Setters (necessarios para JPA y para el adapter)

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getMedicalRecordNumber() {
        return medicalRecordNumber;
    }

    public void setMedicalRecordNumber(String medicalRecordNumber) {
        this.medicalRecordNumber = medicalRecordNumber;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PatientJpaEntity that = (PatientJpaEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}