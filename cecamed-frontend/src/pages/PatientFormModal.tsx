import { useEffect, useState, type FormEvent } from 'react';
import { Save } from 'lucide-react';
import { Modal } from '../components/Modal';
import { Field, Spinner } from '../components/Common';
import { useToast } from '../components/Toast';
import { patientsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { BloodType, Gender, Patient, PatientRequest } from '../api/types';
import { BLOOD_TYPE_LONG, GENDER_LABEL } from '../utils/labels';
import { blankToNull, clinicNow, toIsoDate } from '../utils/format';

interface Props {
  open: boolean;
  patient: Patient | null; // null = registrar nuevo
  onClose: () => void;
  onSaved: (patient: Patient) => void;
}

interface FormState {
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: Gender | '';
  bloodType: BloodType;
  phone: string;
  email: string;
  address: string;
  emergencyContactName: string;
  emergencyContactPhone: string;
  emergencyContactRelationship: string;
  notes: string;
}

const EMPTY: FormState = {
  firstName: '',
  lastName: '',
  birthDate: '',
  gender: '',
  bloodType: 'DESCONOCIDO',
  phone: '',
  email: '',
  address: '',
  emergencyContactName: '',
  emergencyContactPhone: '',
  emergencyContactRelationship: '',
  notes: '',
};

function fromPatient(p: Patient): FormState {
  return {
    firstName: p.firstName ?? '',
    lastName: p.lastName ?? '',
    birthDate: p.birthDate ?? '',
    gender: p.gender ?? '',
    bloodType: p.bloodType ?? 'DESCONOCIDO',
    phone: p.phone ?? '',
    email: p.email ?? '',
    address: p.address ?? '',
    emergencyContactName: p.emergencyContactName ?? '',
    emergencyContactPhone: p.emergencyContactPhone ?? '',
    emergencyContactRelationship: p.emergencyContactRelationship ?? '',
    notes: p.notes ?? '',
  };
}

/** Equivalente de patient-form-dialog.fxml / PatientFormDialogController. */
export function PatientFormModal({ open, patient, onClose, onSaved }: Props) {
  const toast = useToast();
  const [form, setForm] = useState<FormState>(EMPTY);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const isEdit = patient !== null;
  const today = toIsoDate(clinicNow());

  useEffect(() => {
    if (open) {
      setForm(patient ? fromPatient(patient) : EMPTY);
      setError(null);
    }
  }, [open, patient]);

  const set = <K extends keyof FormState>(key: K, value: FormState[K]) => setForm((f) => ({ ...f, [key]: value }));

  const validate = (): string | null => {
    if (!form.firstName.trim()) return 'El nombre del paciente es obligatorio';
    if (!form.lastName.trim()) return 'El apellido del paciente es obligatorio';
    if (!form.birthDate) return 'La fecha de nacimiento es obligatoria';
    if (form.birthDate > today) return 'La fecha de nacimiento no puede ser futura';
    if (!form.gender) return 'El género/sexo es obligatorio';
    if (form.email.trim() && !/^[A-Za-z0-9+_.-]+@(.+)$/.test(form.email.trim()))
      return 'El formato del correo electrónico es inválido';
    return null;
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const validation = validate();
    if (validation) {
      setError(validation);
      return;
    }
    const body: PatientRequest = {
      version: patient?.version,
      firstName: form.firstName.trim(),
      lastName: form.lastName.trim(),
      birthDate: form.birthDate,
      gender: form.gender as Gender,
      bloodType: form.bloodType,
      phone: blankToNull(form.phone),
      email: blankToNull(form.email),
      address: blankToNull(form.address),
      emergencyContactName: blankToNull(form.emergencyContactName),
      emergencyContactPhone: blankToNull(form.emergencyContactPhone),
      emergencyContactRelationship: blankToNull(form.emergencyContactRelationship),
      notes: blankToNull(form.notes),
    };

    setSaving(true);
    setError(null);
    try {
      const saved = isEdit ? await patientsApi.update(patient.id, body) : await patientsApi.create(body);
      if (isEdit) {
        toast.success('Paciente actualizado', `Los datos de ${saved.fullName} fueron actualizados correctamente`);
      } else {
        toast.success(
          'Paciente registrado',
          `El paciente ${saved.fullName} fue registrado exitosamente con el expediente ${saved.medicalRecordNumber ?? ''}`,
        );
      }
      onSaved(saved);
    } catch (err) {
      setError(errorMessage(err, 'No fue posible guardar el paciente'));
    } finally {
      setSaving(false);
    }
  };

  return (
    <Modal
      open={open}
      onClose={onClose}
      locked={saving}
      size="lg"
      title={isEdit ? `Editar paciente: ${patient.fullName}` : 'Registrar paciente'}
      subtitle="Los campos marcados con * son obligatorios"
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={saving}>
            Cancelar
          </button>
          <button type="submit" form="patient-form" className="btn btn-primary" disabled={saving}>
            {saving ? <Spinner size={16} /> : <Save size={16} aria-hidden />}
            {isEdit ? 'Guardar cambios' : 'Guardar paciente'}
          </button>
        </>
      }
    >
      <form id="patient-form" onSubmit={onSubmit} noValidate>
        <fieldset className="form-section" disabled={saving}>
          <legend>Datos personales</legend>
          <div className="form-grid">
            <Field label="Nombres" htmlFor="pf-first" required>
              <input id="pf-first" value={form.firstName} maxLength={100} onChange={(e) => set('firstName', e.target.value)} />
            </Field>
            <Field label="Apellidos" htmlFor="pf-last" required>
              <input id="pf-last" value={form.lastName} maxLength={100} onChange={(e) => set('lastName', e.target.value)} />
            </Field>
            <Field label="Fecha de nacimiento" htmlFor="pf-birth" required>
              <input id="pf-birth" type="date" max={today} value={form.birthDate} onChange={(e) => set('birthDate', e.target.value)} />
            </Field>
            <Field label="Sexo" htmlFor="pf-gender" required>
              <select id="pf-gender" value={form.gender} onChange={(e) => set('gender', e.target.value as Gender | '')}>
                <option value="">Seleccione...</option>
                {(Object.keys(GENDER_LABEL) as Gender[]).map((g) => (
                  <option key={g} value={g}>
                    {GENDER_LABEL[g]}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Grupo sanguíneo" htmlFor="pf-blood">
              <select id="pf-blood" value={form.bloodType} onChange={(e) => set('bloodType', e.target.value as BloodType)}>
                {(Object.keys(BLOOD_TYPE_LONG) as BloodType[]).map((b) => (
                  <option key={b} value={b}>
                    {BLOOD_TYPE_LONG[b]}
                  </option>
                ))}
              </select>
            </Field>
          </div>
        </fieldset>

        <fieldset className="form-section" disabled={saving}>
          <legend>Contacto</legend>
          <div className="form-grid">
            <Field label="Teléfono" htmlFor="pf-phone">
              <input id="pf-phone" type="tel" value={form.phone} maxLength={25} onChange={(e) => set('phone', e.target.value)} />
            </Field>
            <Field label="Correo electrónico" htmlFor="pf-email">
              <input id="pf-email" type="email" value={form.email} maxLength={100} onChange={(e) => set('email', e.target.value)} />
            </Field>
            <Field label="Dirección" htmlFor="pf-address" className="span-2">
              <textarea id="pf-address" rows={2} value={form.address} maxLength={255} onChange={(e) => set('address', e.target.value)} />
            </Field>
          </div>
        </fieldset>

        <fieldset className="form-section" disabled={saving}>
          <legend>Contacto de emergencia</legend>
          <div className="form-grid form-grid-3">
            <Field label="Nombre" htmlFor="pf-em-name">
              <input id="pf-em-name" value={form.emergencyContactName} maxLength={100} onChange={(e) => set('emergencyContactName', e.target.value)} />
            </Field>
            <Field label="Teléfono" htmlFor="pf-em-phone">
              <input id="pf-em-phone" type="tel" value={form.emergencyContactPhone} maxLength={25} onChange={(e) => set('emergencyContactPhone', e.target.value)} />
            </Field>
            <Field label="Parentesco" htmlFor="pf-em-rel">
              <input id="pf-em-rel" value={form.emergencyContactRelationship} maxLength={50} onChange={(e) => set('emergencyContactRelationship', e.target.value)} />
            </Field>
          </div>
        </fieldset>

        <fieldset className="form-section" disabled={saving}>
          <legend>Notas</legend>
          <Field label="Notas adicionales" htmlFor="pf-notes">
            <textarea id="pf-notes" rows={3} value={form.notes} onChange={(e) => set('notes', e.target.value)} />
          </Field>
        </fieldset>

        {error && (
          <div className="alert alert-danger" role="alert">
            {error}
          </div>
        )}
      </form>
    </Modal>
  );
}
