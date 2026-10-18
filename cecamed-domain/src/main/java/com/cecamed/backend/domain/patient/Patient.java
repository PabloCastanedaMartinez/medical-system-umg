package com.cecamed.backend.domain.patient;

import com.cecamed.backend.domain.shared.BusinessRuleException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Entidad de dominio que representa a un paciente.
 * Contiene los datos y las reglas de negocio del paciente.
 */
public class Patient {

    private Long id;
    private String identificationNumber; // normalizado, puede ser null
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private String gender;
    private String bloodType;
    private String phone;
    private String email;
    private String address;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String notes;
    private boolean active;
    private String medicalRecordNumber;

    public Patient() {
        // Constructor público para frameworks
    }

    /**
     * Crea una nueva instancia de Patient a partir de los datos proporcionados.
     * Normaliza el número de identificación y genera el número de expediente médico.
     *
     * @param clock               reloj para validar que la fecha de nacimiento no sea futura
     * @param identificationNumber número de identificación (opcional, se normaliza)
     * @param firstName           nombre(s)
     * @param lastName            apellido(s)
     * @param birthDate           fecha de nacimiento
     * @param gender              género/sexo
     * @param bloodType           grupo sanguíneo
     * @param phone               teléfono (opcional)
     * @param email               correo electrónico (opcional)
     * @param address             dirección (opcional)
     * @param emergencyContactName nombre del contacto de emergencia (opcional)
     * @param emergencyContactPhone teléfono del contacto de emergencia (opcional)
     * @param emergencyContactRelationship relación del contacto de emergencia (opcional)
     * @param notes               notas adicionales (opcional)
     * @return nueva instancia de Patient
     */
    public static Patient createNew(
            Clock clock,
            String identificationNumber,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String gender,
            String bloodType,
            String phone,
            String email,
            String address,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String notes) {

        Objects.requireNonNull(clock, "clock no puede ser nulo");
        Objects.requireNonNull(firstName, "firstName no puede ser nulo");
        Objects.requireNonNull(lastName, "lastName no puede ser nulo");
        Objects.requireNonNull(birthDate, "birthDate no puede ser nulo");
        Objects.requireNonNull(gender, "gender no puede ser nulo");
        Objects.requireNonNull(bloodType, "bloodType no puede ser nulo");

        // Validar que la fecha de nacimiento no sea futura
        LocalDate today = LocalDate.now(clock);
        if (birthDate.isAfter(today)) {
            throw new BusinessRuleException("PATIENT_FUTURE_BIRTH_DATE",
                    "La fecha de nacimiento no puede ser una fecha futura");
        }

        // Normalizar el número de identificación si se proporciona
        String normalizedId = normalizeIdentificationNumber(identificationNumber);

        // Validar que nombre y apellido no estén en blanco después de trim
        if (firstName.trim().isEmpty()) {
            throw new BusinessRuleException("PATIENT_FIRST_NAME_BLANK",
                    "El nombre del paciente no puede estar vacío");
        }
        if (lastName.trim().isEmpty()) {
            throw new BusinessRuleException("PATIENT_LAST_NAME_BLANK",
                    "El apellido del paciente no puede estar vacío");
        }

        // Generar número de expediente médico (UUID simplificado)
        String medicalRecordNumber = UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        Patient patient = new Patient();
        patient.id = null; // Nuevo paciente, sin ID aún
        patient.identificationNumber = normalizedId;
        patient.firstName = firstName.trim();
        patient.lastName = lastName.trim();
        patient.fullName = patient.firstName + " " + patient.lastName;
        patient.birthDate = birthDate;
        patient.gender = gender.toUpperCase(); // asegurar mayúsculas
        patient.bloodType = bloodType.toUpperCase(); // asegurar mayúsculas
        patient.phone = phone != null ? phone.trim() : null;
        patient.email = email != null ? email.trim() : null;
        patient.address = address != null ? address.trim() : null;
        patient.emergencyContactName = emergencyContactName != null ? emergencyContactName.trim() : null;
        patient.emergencyContactPhone = emergencyContactPhone != null ? emergencyContactPhone.trim() : null;
        patient.emergencyContactRelationship = emergencyContactRelationship != null ? emergencyContactRelationship.trim() : null;
        patient.notes = notes != null ? notes.trim() : null;
        patient.active = true;
        patient.medicalRecordNumber = medicalRecordNumber;

        return patient;
    }

    /**
     * Actualiza la información del paciente (excepto número de identificación y expediente médico).
     * Lanza PatientInactiveException si el paciente está inactivo.
     *
     * @param clock               reloj para validar que la fecha de nacimiento no sea futura
     * @param firstName           nombre(s)
     * @param lastName            apellido(s)
     * @param birthDate           fecha de nacimiento
     * @param gender              género/sexo
     * @param bloodType           grupo sanguíneo
     * @param phone               teléfono (opcional)
     * @param email               correo electrónico (opcional)
     * @param address             dirección (opcional)
     * @param emergencyContactName nombre del contacto de emergencia (opcional)
     * @param emergencyContactPhone teléfono del contacto de emergencia (opcional)
     * @param emergencyContactRelationship relación del contacto de emergencia (opcional)
     * @param notes               notas adicionales (opcional)
     */
    public void updateInformation(
            Clock clock,
            String firstName,
            String lastName,
            LocalDate birthDate,
            String gender,
            String bloodType,
            String phone,
            String email,
            String address,
            String emergencyContactName,
            String emergencyContactPhone,
            String emergencyContactRelationship,
            String notes) {

        if (!this.active) {
            throw new PatientInactiveException("No se puede actualizar un paciente inactivo");
        }

        Objects.requireNonNull(clock, "clock no puede ser nulo");
        Objects.requireNonNull(firstName, "firstName no puede ser nulo");
        Objects.requireNonNull(lastName, "lastName no puede ser nulo");
        Objects.requireNonNull(birthDate, "birthDate no puede ser nulo");
        Objects.requireNonNull(gender, "gender no puede ser nulo");
        Objects.requireNonNull(bloodType, "bloodType no puede ser nulo");

        // Validar que la fecha de nacimiento no sea futura
        LocalDate today = LocalDate.now(clock);
        if (birthDate.isAfter(today)) {
            throw new BusinessRuleException("PATIENT_FUTURE_BIRTH_DATE",
                    "La fecha de nacimiento no puede ser una fecha futura");
        }

        // Validar que nombre y apellido no estén en blanco después de trim
        if (firstName.trim().isEmpty()) {
            throw new BusinessRuleException("PATIENT_FIRST_NAME_BLANK",
                    "El nombre del paciente no puede estar vacío");
        }
        if (lastName.trim().isEmpty()) {
            throw new BusinessRuleException("PATIENT_LAST_NAME_BLANK",
                    "El apellido del paciente no puede estar vacío");
        }

        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.fullName = this.firstName + " " + this.lastName;
        this.birthDate = birthDate;
        this.gender = gender.toUpperCase();
        this.bloodType = bloodType.toUpperCase();
        this.phone = phone != null ? phone.trim() : null;
        this.email = email != null ? email.trim() : null;
        this.address = address != null ? address.trim() : null;
        this.emergencyContactName = emergencyContactName != null ? emergencyContactName.trim() : null;
        this.emergencyContactPhone = emergencyContactPhone != null ? emergencyContactPhone.trim() : null;
        this.emergencyContactRelationship = emergencyContactRelationship != null ? emergencyContactRelationship.trim() : null;
        this.notes = notes != null ? notes.trim() : null;
    }

    /**
     * Activa el paciente.
     * Lanza PatientInactiveException si ya está activo.
     *
     * @param clock reloj para establecer la fecha de actualización (no se usa en dominio, pero se mantiene por consistencia)
     */
    public void activate(Clock clock) {
        if (this.active) {
            throw new BusinessRuleException("PATIENT_ALREADY_ACTIVE", "El paciente ya está activo");
        }
        this.active = true;
        // En una implementación real, se actualizaría un timestamp de actualización, pero eso pertenece a la capa de infraestructura
    }

    /**
     * Desactiva el paciente.
     * Lanza PatientInactiveException si ya está inactivo.
     *
     * @param clock reloj para establecer la fecha de actualización (no se usa en dominio, pero se mantiene por consistencia)
     */
    public void deactivate(Clock clock) {
        if (!this.active) {
            throw new BusinessRuleException("PATIENT_ALREADY_INACTIVE", "El paciente ya está inactivo");
        }
        this.active = false;
    }

    // Getters y Setters (solo los necesarios para el mapeo y la capa de aplicación)

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdentificationNumber() {
        return identificationNumber;
    }

    /**
     * Establece el número de identificación.
     * Nota: este setter existe únicamente para propósitos de mapeo ORM.
     * Según las reglas de negocio, el número de identificación no debería cambiarse después de la creación.
     *
     * @param identificationNumber número de identificación a establecer
     */
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

    /**
     * Normaliza el número de identificación: elimina espacios en exceso, quita guiones y convierte a mayúsculas.
     * Si el resultado está vacío, devuelve null.
     *
     * @param identificationNumber número de identificación a normalizar
     * @return número normalizado o null
     */
    private static String normalizeIdentificationNumber(String identificationNumber) {
        if (identificationNumber == null) {
            return null;
        }
        String trimmed = identificationNumber.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Eliminar guiones y espacios múltiples
        String noHyphens = trimmed.replaceAll("-", "");
        String collapsedSpaces = noHyphens.replaceAll("\\s+", " ");
        String trimmedAgain = collapsedSpaces.trim();
        if (trimmedAgain.isEmpty()) {
            return null;
        }
        return trimmedAgain.toUpperCase();
    }
}