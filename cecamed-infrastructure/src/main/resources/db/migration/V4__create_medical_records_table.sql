-- Medical Records Table
-- Stores the clinical record for each patient (one record per patient)

CREATE TABLE cecamed.medical_records (
    patient_id BIGINT NOT NULL,
    record_number VARCHAR(50) NOT NULL,
    allergies TEXT,
    pathological_history TEXT,
    non_pathological_history TEXT,
    family_history TEXT,
    surgical_history TEXT,
    traumatic_history TEXT,
    current_medications TEXT,
    general_observations TEXT,
    gynecological_obstetric_history TEXT,
    water_glasses_per_day INTEGER,
    meals_per_day INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    
    PRIMARY KEY (patient_id),
    UNIQUE (record_number),
    
    -- Foreign key to patient table (assuming it exists in Fase 3)
    CONSTRAINT fk_medical_records_patient
        FOREIGN KEY (patient_id)
        REFERENCES cecamed.patients(id)
        ON DELETE CASCADE
);

-- Index for looking up records by patient (already covered by PK)
-- Index for looking up records by record number (already covered by unique constraint)

COMMENT ON TABLE cecamed.medical_records IS 'Expediente clínico de los pacientes';
COMMENT ON COLUMN cecamed.medical_records.patient_id IS 'Identificador del paciente (FK a la tabla de pacientes)';
COMMENT ON COLUMN cecamed.medical_records.record_number IS 'Número único del expediente';
COMMENT ON COLUMN cecamed.medical_records.allergies IS 'Alergias del paciente';
COMMENT ON COLUMN cecamed.medical_records.pathological_history IS 'Historia patológica';
COMMENT ON COLUMN cecamed.medical_records.non_pathological_history IS 'Historia no patológica';
COMMENT ON COLUMN cecamed.medical_records.family_history IS 'Historia familiar';
COMMENT ON COLUMN cecamed.medical_records.surgical_history IS 'Historia quirúrgica';
COMMENT ON COLUMN cecamed.medical_records.traumatic_history IS 'Historia traumática';
COMMENT ON COLUMN cecamed.medical_records.current_medications IS 'Medicación actual';
COMMENT ON COLUMN cecamed.medical_records.general_observations IS 'Observaciones generales';
COMMENT ON COLUMN cecamed.medical_records.gynecological_obstetric_history IS 'Historia gineco-obstétrica';
COMMENT ON COLUMN cecamed.medical_records.water_glasses_per_day IS 'Vasos de agua por día';
COMMENT ON COLUMN cecamed.medical_records.meals_per_day IS 'Comidas por día';
COMMENT ON COLUMN cecamed.medical_records.created_at IS 'Fecha de creación del registro';
COMMENT ON COLUMN cecamed.medical_records.updated_at IS 'Fecha de última actualización';
COMMENT ON COLUMN cecamed.medical_records.version IS 'Versión para bloqueo optimista';