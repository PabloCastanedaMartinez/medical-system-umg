-- Consultations Table
-- Stores medical consultations for patients (multiple consultations per patient)

CREATE TABLE cecamed.consultations (
    consultation_id BIGINT NOT NULL,
    patient_id BIGINT NOT NULL,
    consultation_date TIMESTAMP WITH TIME ZONE NOT NULL,
    consultation_type VARCHAR(100),
    doctor_name VARCHAR(200),
    reason_for_visit TEXT,
    symptoms TEXT,
    examination_findings TEXT,
    diagnosis TEXT,
    treatment_plan TEXT,
    prescriptions TEXT,
    follow_up_instructions TEXT,
    vital_signs TEXT,
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    PRIMARY KEY (consultation_id),

    -- Foreign key to patient table (assuming it exists in Fase 3)
    CONSTRAINT fk_consultations_patient
        FOREIGN KEY (patient_id)
        REFERENCES cecamed.patients(id)
        ON DELETE CASCADE
);

-- Index for looking up consultations by patient (frequently queried)
CREATE INDEX idx_consultations_patient_id ON cecamed.consultations(patient_id);

-- Index for looking up consultations by date range (frequently queried)
CREATE INDEX idx_consultations_date ON cecamed.consultations(consultation_date);

-- Composite index for patient + date (common query pattern)
CREATE INDEX idx_consultations_patient_date ON cecamed.consultations(patient_id, consultation_date);

COMMENT ON TABLE cecamed.consultations IS 'Consultas médicas de los pacientes';
COMMENT ON COLUMN cecamed.consultations.consultation_id IS 'Identificador único de la consulta';
COMMENT ON COLUMN cecamed.consultations.patient_id IS 'Identificador del paciente (FK a la tabla de pacientes)';
COMMENT ON COLUMN cecamed.consultations.consultation_date IS 'Fecha y hora de la consulta';
COMMENT ON COLUMN cecamed.consultations.consultation_type IS 'Tipo de consulta (general, seguimiento, emergencia, etc.)';
COMMENT ON COLUMN cecamed.consultations.doctor_name IS 'Nombre del médico que atendió la consulta';
COMMENT ON COLUMN cecamed.consultations.reason_for_visit IS 'Motivo de la consulta';
COMMENT ON COLUMN cecamed.consultations.symptoms IS 'Síntomas reportados por el paciente';
COMMENT ON COLUMN cecamed.consultations.examination_findings IS 'Hallazgos del examen físico';
COMMENT ON COLUMN cecamed.consultations.diagnosis IS 'Diagnóstico médico';
COMMENT ON COLUMN cecamed.consultations.treatment_plan IS 'Plan de tratamiento';
COMMENT ON COLUMN cecamed.consultations.prescriptions IS 'Medicaciones recetadas';
COMMENT ON COLUMN cecamed.consultations.follow_up_instructions IS 'Instrucciones de seguimiento';
COMMENT ON COLUMN cecamed.consultations.vital_signs IS 'Signos vitales (presión arterial, temperatura, etc.)';
COMMENT ON COLUMN cecamed.consultations.notes IS 'Notas adicionales del médico';
COMMENT ON COLUMN cecamed.consultations.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN cecamed.consultations.updated_at IS 'Fecha de última actualización';
COMMENT ON COLUMN cecamed.consultations.version IS 'Versión para bloqueo optimista';