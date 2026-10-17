import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { useNavigate, useParams } from 'react-router';
import { AlertTriangle, ArrowLeft, CalendarDays, ClipboardList, FileText, Save, Stethoscope } from 'lucide-react';
import { medicalRecordApi, patientsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Appointment, Consultation, ConsultationRequest, MedicalRecord, Patient } from '../api/types';
import { useToast } from '../components/Toast';
import { AppointmentStatusBadge, Badge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, Field, PageHeader, Spinner } from '../components/Common';
import { useApi } from '../utils/useApi';
import {
  blankToNull,
  formatDate,
  formatDateTime,
  formatTime,
  parseDecimalOrNull,
  parseIntOrNull,
  toIsoDate,
  toIsoDateTime,
} from '../utils/format';
import { BLOOD_TYPE_SHORT, GENDER_LABEL } from '../utils/labels';
import { DocumentsTab } from './DocumentsTab';

type Tab = 'pathological' | 'nonPathological' | 'consultations' | 'newConsultation' | 'appointments' | 'documents';

const TABS: { id: Tab; label: string }[] = [
  { id: 'pathological', label: 'Antecedentes patológicos' },
  { id: 'nonPathological', label: 'Antecedentes no patológicos' },
  { id: 'consultations', label: 'Consultas y signos vitales' },
  { id: 'newConsultation', label: 'Nueva consulta' },
  { id: 'appointments', label: 'Historial de citas' },
  { id: 'documents', label: 'Documentos' },
];

interface RecordData {
  patient: Patient;
  record: MedicalRecord;
  consultations: Consultation[];
  appointments: Appointment[];
}

type RecordForm = Record<
  | 'allergies'
  | 'currentMedications'
  | 'pathologicalHistory'
  | 'nonPathologicalHistory'
  | 'familyHistory'
  | 'surgicalHistory'
  | 'traumaticHistory'
  | 'generalObservations'
  | 'gynecologicalObstetricHistory'
  | 'waterGlassesPerDay'
  | 'mealsPerDay',
  string
>;

function toRecordForm(r: MedicalRecord | null): RecordForm {
  return {
    allergies: r?.allergies ?? '',
    currentMedications: r?.currentMedications ?? '',
    pathologicalHistory: r?.pathologicalHistory ?? '',
    nonPathologicalHistory: r?.nonPathologicalHistory ?? '',
    familyHistory: r?.familyHistory ?? '',
    surgicalHistory: r?.surgicalHistory ?? '',
    traumaticHistory: r?.traumaticHistory ?? '',
    generalObservations: r?.generalObservations ?? '',
    gynecologicalObstetricHistory: r?.gynecologicalObstetricHistory ?? '',
    waterGlassesPerDay: r?.waterGlassesPerDay != null ? String(r.waterGlassesPerDay) : '',
    mealsPerDay: r?.mealsPerDay != null ? String(r.mealsPerDay) : '',
  };
}

/** Selector de expediente: sin :patientId abre el primer paciente activo (como en escritorio). */
export function MedicalRecordIndex() {
  const navigate = useNavigate();
  const { data, loading, error, reload } = useApi(() => patientsApi.list('', 'ACTIVE'), []);

  useEffect(() => {
    if (data && data.length > 0) navigate(`/expediente/${data[0].id}`, { replace: true });
  }, [data, navigate]);

  if (error) return <ErrorBanner message={error} onRetry={reload} />;
  if (loading || (data && data.length > 0)) {
    return (
      <div className="center-pad">
        <Spinner />
      </div>
    );
  }
  return <EmptyState icon={ClipboardList} message="No hay pacientes registrados" />;
}

/** Equivalente de medical-record.fxml / MedicalRecordController. */
export function MedicalRecordPage() {
  const { patientId: idParam } = useParams();
  const patientId = Number(idParam);
  const navigate = useNavigate();
  const toast = useToast();

  const [tab, setTab] = useState<Tab>('pathological');
  const [selectedConsultationId, setSelectedConsultationId] = useState<number | null>(null);
  const [recordForm, setRecordForm] = useState<RecordForm>(toRecordForm(null));
  const [savingRecord, setSavingRecord] = useState(false);

  const { data, loading, error, reload, setData } = useApi<RecordData>(
    async () => {
      const [patient, record, consultations, appointments] = await Promise.all([
        patientsApi.get(patientId),
        medicalRecordApi.get(patientId),
        medicalRecordApi.consultations(patientId),
        patientsApi.appointments(patientId),
      ]);
      return { patient, record, consultations, appointments };
    },
    [patientId],
    Number.isFinite(patientId),
  );
  const { data: patientOptions } = useApi(() => patientsApi.list('', 'ACTIVE'), []);

  useEffect(() => {
    if (!data) return;
    setRecordForm(toRecordForm(data.record));
    setSelectedConsultationId(data.consultations[0]?.id ?? null);
  }, [data?.record, data?.consultations]); // eslint-disable-line react-hooks/exhaustive-deps

  const dirty = useMemo(
    () => data != null && JSON.stringify(recordForm) !== JSON.stringify(toRecordForm(data.record)),
    [recordForm, data],
  );

  const setField = (key: keyof RecordForm, value: string) => setRecordForm((f) => ({ ...f, [key]: value }));

  const saveRecord = async () => {
    if (!data) return;
    const water = recordForm.waterGlassesPerDay.trim();
    const meals = recordForm.mealsPerDay.trim();
    const validCount = (v: string) => v === '' || /^\d+$/.test(v);
    if (!validCount(water) || !validCount(meals)) {
      toast.warning(
        'Datos inválidos',
        'Los vasos de agua y las comidas al día deben ser números enteros mayores o iguales a cero, o quedar vacíos.',
      );
      return;
    }
    const body: MedicalRecord = {
      ...data.record,
      patientId,
      recordNumber: data.record.recordNumber ?? data.patient.medicalRecordNumber ?? '',
      allergies: recordForm.allergies.trim(),
      currentMedications: recordForm.currentMedications.trim(),
      pathologicalHistory: recordForm.pathologicalHistory.trim(),
      nonPathologicalHistory: recordForm.nonPathologicalHistory.trim(),
      familyHistory: recordForm.familyHistory.trim(),
      surgicalHistory: recordForm.surgicalHistory.trim(),
      traumaticHistory: recordForm.traumaticHistory.trim(),
      generalObservations: recordForm.generalObservations.trim(),
      gynecologicalObstetricHistory: recordForm.gynecologicalObstetricHistory.trim(),
      waterGlassesPerDay: water === '' ? null : Number(water),
      mealsPerDay: meals === '' ? null : Number(meals),
    };
    setSavingRecord(true);
    try {
      const saved = await medicalRecordApi.update(patientId, body);
      setData((prev) => (prev ? { ...prev, record: saved } : prev));
      toast.success('Expediente actualizado', 'Los antecedentes clínicos fueron guardados satisfactoriamente');
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible actualizar los antecedentes del expediente'));
    } finally {
      setSavingRecord(false);
    }
  };

  if (!Number.isFinite(patientId)) {
    return <EmptyState icon={ClipboardList} message="Seleccione un paciente válido" />;
  }

  const patient = data?.patient;
  const allergies = data?.record.allergies?.trim();
  const selectedConsultation = data?.consultations.find((c) => c.id === selectedConsultationId) ?? null;

  const textArea = (key: keyof RecordForm, label: string, placeholder: string, rows = 3) => (
    <Field label={label} htmlFor={`rec-${key}`}>
      <textarea id={`rec-${key}`} rows={rows} placeholder={placeholder} value={recordForm[key]} onChange={(e) => setField(key, e.target.value)} />
    </Field>
  );

  const saveBar = (
    <div className="form-actions">
      {dirty && <span className="muted small">Hay cambios sin guardar</span>}
      <button className="btn btn-primary" onClick={saveRecord} disabled={savingRecord || !data}>
        {savingRecord ? <Spinner size={16} /> : <Save size={16} aria-hidden />} Guardar antecedentes
      </button>
    </div>
  );

  return (
    <div className="page">
      <PageHeader
        title="Expediente clínico"
        actions={
          <button className="btn btn-ghost" onClick={() => navigate('/pacientes')}>
            <ArrowLeft size={16} aria-hidden /> Volver a pacientes
          </button>
        }
      />

      <section className="card patient-header">
        <div className="patient-header-main">
          {patient ? (
            <>
              <h2>{patient.fullName}</h2>
              <div className="patient-meta">
                <span>Edad: {patient.age != null ? `${patient.age} años` : '--'}</span>
                <span>Sexo: {patient.gender ? GENDER_LABEL[patient.gender] : '--'}</span>
                <span className="mono">Expediente: {patient.medicalRecordNumber ?? '--'}</span>
                <Badge tone="info">Grupo: {patient.bloodType ? BLOOD_TYPE_SHORT[patient.bloodType] : 'N/D'}</Badge>
              </div>
              <div className={`allergy-line${allergies ? ' has-allergies' : ''}`}>
                {allergies && <AlertTriangle size={14} aria-hidden />}
                <strong>Alergias:</strong> {allergies || 'Ninguna reportada'}
              </div>
            </>
          ) : loading ? (
            <Spinner />
          ) : null}
        </div>
        <div className="field patient-switch">
          <label htmlFor="patient-switch">Cambiar de paciente</label>
          <select
            id="patient-switch"
            value={patientId}
            onChange={(e) => {
              if (dirty && !window.confirm('Hay antecedentes sin guardar. ¿Desea cambiar de paciente de todas formas?')) return;
              navigate(`/expediente/${e.target.value}`);
              setTab('pathological');
            }}
          >
            {patient && !patientOptions?.some((p) => p.id === patient.id) && (
              <option value={patient.id}>{patient.fullName}</option>
            )}
            {(patientOptions ?? []).map((p) => (
              <option key={p.id} value={p.id}>
                {p.fullName} (Exp.: {p.medicalRecordNumber ?? '-'})
              </option>
            ))}
          </select>
        </div>
      </section>

      {error && <ErrorBanner message={error} onRetry={reload} />}

      <div className="tabs" role="tablist" aria-label="Secciones del expediente">
        {TABS.map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} className={tab === t.id ? 'active' : ''} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>

      <section className="card tab-panel" role="tabpanel">
        {!data && loading ? (
          <div className="center-pad">
            <Spinner />
          </div>
        ) : !data ? null : tab === 'pathological' ? (
          <>
            <div className="form-grid">
              {textArea('allergies', 'Alérgicos', "Medicamentos, alimentos y otras alergias, o 'Ninguna conocida'")}
              {textArea('currentMedications', 'Medicación actual / Uso continuo', 'Fármacos actuales, dosis y frecuencia')}
              {textArea('pathologicalHistory', 'Médicos', 'Enfermedades previas y crónicas: hipertensión, diabetes, asma...')}
              {textArea('familyHistory', 'Familiares', 'Enfermedades en padres, abuelos o hermanos...')}
              {textArea('surgicalHistory', 'Quirúrgicos', 'Cirugías y hospitalizaciones previas...')}
              {textArea('traumaticHistory', 'Traumáticos', 'Fracturas, accidentes, lesiones y secuelas...')}
            </div>
            {textArea('generalObservations', 'Observaciones clínicas generales', 'Notas adicionales del expediente...')}
            {saveBar}
          </>
        ) : tab === 'nonPathological' ? (
          <>
            {textArea('nonPathologicalHistory', 'Hábitos o manías', 'Sueño, tabaquismo, alcohol, actividad física y otros hábitos...')}
            <div className="form-grid">
              <Field label="Vasos de agua al día" htmlFor="rec-water">
                <input id="rec-water" inputMode="numeric" placeholder="Ej. 8" value={recordForm.waterGlassesPerDay} onChange={(e) => setField('waterGlassesPerDay', e.target.value)} />
              </Field>
              <Field label="Comidas al día" htmlFor="rec-meals">
                <input id="rec-meals" inputMode="numeric" placeholder="Ej. 3" value={recordForm.mealsPerDay} onChange={(e) => setField('mealsPerDay', e.target.value)} />
              </Field>
            </div>
            {textArea('gynecologicalObstetricHistory', 'Ginecoobstétricos', 'Menstruación, embarazos, partos, cesáreas, abortos...')}
            {saveBar}
          </>
        ) : tab === 'consultations' ? (
          <ConsultationsTab
            consultations={data.consultations}
            selected={selectedConsultation}
            onSelect={setSelectedConsultationId}
            onNew={() => setTab('newConsultation')}
          />
        ) : tab === 'newConsultation' ? (
          <NewConsultationTab
            patientId={patientId}
            onCreated={(created) => {
              setData((prev) => (prev ? { ...prev, consultations: [created, ...prev.consultations] } : prev));
              setSelectedConsultationId(created.id);
              setTab('consultations');
            }}
          />
        ) : tab === 'appointments' ? (
          <AppointmentsHistory appointments={data.appointments} />
        ) : (
          <DocumentsTab patientId={patientId} />
        )}
      </section>
    </div>
  );
}

function ConsultationsTab({
  consultations,
  selected,
  onSelect,
  onNew,
}: {
  consultations: Consultation[];
  selected: Consultation | null;
  onSelect: (id: number) => void;
  onNew: () => void;
}) {
  if (consultations.length === 0) {
    return (
      <EmptyState
        icon={Stethoscope}
        message="El paciente aún no tiene consultas registradas"
        action={
          <button className="btn btn-primary" onClick={onNew}>
            Registrar consulta
          </button>
        }
      />
    );
  }
  const v = selected?.vitalSigns;
  const val = (x: number | null | undefined, unit: string) => (x != null ? `${x} ${unit}` : '--');
  return (
    <div className="consultations-layout">
      <div className="table-wrap">
        <h3 className="section-title">Consultas realizadas</h3>
        <table className="table table-hover table-selectable">
          <thead>
            <tr>
              <th>Fecha/Hora</th>
              <th>Motivo</th>
              <th>Diagnóstico</th>
              <th>CIE-10</th>
            </tr>
          </thead>
          <tbody>
            {consultations.map((c) => (
              <tr
                key={c.id}
                className={selected?.id === c.id ? 'selected' : ''}
                onClick={() => onSelect(c.id)}
                tabIndex={0}
                onKeyDown={(e) => (e.key === 'Enter' || e.key === ' ') && onSelect(c.id)}
                aria-selected={selected?.id === c.id}
              >
                <td className="nowrap">{formatDateTime(c.consultationDateTime)}</td>
                <td>{c.reason}</td>
                <td>{c.diagnosis}</td>
                <td className="mono">{c.icd10Code || '-'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {selected && (
        <aside className="consultation-detail">
          <h3 className="section-title">Detalle de la consulta</h3>
          <dl className="detail-list">
            <dt>Fecha</dt>
            <dd>{formatDateTime(selected.consultationDateTime)}</dd>
            <dt>Motivo de la consulta</dt>
            <dd>{selected.reason || '-'}</dd>
            {selected.symptoms && (
              <>
                <dt>Síntomas</dt>
                <dd className="pre">{selected.symptoms}</dd>
              </>
            )}
            <dt>Diagnóstico</dt>
            <dd>
              {selected.diagnosis || '-'}
              {selected.icd10Code && <span className="muted"> (CIE-10: {selected.icd10Code})</span>}
            </dd>
            <dt>Plan de tratamiento y receta</dt>
            <dd className="pre">{selected.treatmentPlan || 'Sin plan registrado'}</dd>
            <dt>Examen físico</dt>
            <dd className="pre">{selected.physicalExamination || '-'}</dd>
            {selected.privateNotes && (
              <>
                <dt>Notas privadas del médico</dt>
                <dd className="pre">{selected.privateNotes}</dd>
              </>
            )}
          </dl>
          <h4 className="subsection-title">Signos vitales</h4>
          <div className="vitals-grid">
            <div><span>Presión arterial</span><strong>{v?.systolicPressure != null || v?.diastolicPressure != null ? `${v?.systolicPressure ?? '--'}/${v?.diastolicPressure ?? '--'} mmHg` : '--/--'}</strong></div>
            <div><span>Frecuencia cardíaca</span><strong>{val(v?.heartRate, 'lpm')}</strong></div>
            <div><span>Temperatura</span><strong>{val(v?.temperatureCelsius, '°C')}</strong></div>
            <div><span>Frecuencia respiratoria</span><strong>{val(v?.respiratoryRate, 'rpm')}</strong></div>
            <div><span>Peso</span><strong>{val(v?.weightKg, 'kg')}</strong></div>
            <div><span>Talla</span><strong>{val(v?.heightCm, 'cm')}</strong></div>
            <div><span>IMC</span><strong>{val(v?.bmi, 'kg/m²')}</strong></div>
            <div><span>Saturación de oxígeno</span><strong>{val(v?.oxygenSaturationPercentage, '%')}</strong></div>
          </div>
        </aside>
      )}
    </div>
  );
}

const EMPTY_CONSULTATION = {
  date: '',
  reason: '',
  symptoms: '',
  physicalExamination: '',
  systolic: '',
  diastolic: '',
  heartRate: '',
  respiratoryRate: '',
  temperature: '',
  weight: '',
  height: '',
  spo2: '',
  diagnosis: '',
  icd10: '',
  treatment: '',
  privateNotes: '',
};

function NewConsultationTab({ patientId, onCreated }: { patientId: number; onCreated: (c: Consultation) => void }) {
  const toast = useToast();
  const [form, setForm] = useState({ ...EMPTY_CONSULTATION, date: toIsoDate(new Date()) });
  const [saving, setSaving] = useState(false);
  const set = (key: keyof typeof EMPTY_CONSULTATION, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.reason.trim()) {
      toast.warning('Campo obligatorio', 'El motivo de consulta es requerido');
      document.getElementById('nc-reason')?.focus();
      return;
    }
    if (!form.diagnosis.trim()) {
      toast.warning('Campo obligatorio', 'El diagnóstico clínico es requerido');
      document.getElementById('nc-diagnosis')?.focus();
      return;
    }
    const now = new Date();
    const [y, m, d] = (form.date || toIsoDate(now)).split('-').map(Number);
    const consultationDate = new Date(y, m - 1, d, now.getHours(), now.getMinutes(), now.getSeconds());

    const body: ConsultationRequest = {
      patientId,
      consultationDateTime: toIsoDateTime(consultationDate),
      reason: form.reason.trim(),
      symptoms: blankToNull(form.symptoms),
      physicalExamination: blankToNull(form.physicalExamination),
      vitalSigns: {
        systolicPressure: parseIntOrNull(form.systolic),
        diastolicPressure: parseIntOrNull(form.diastolic),
        heartRate: parseIntOrNull(form.heartRate),
        respiratoryRate: parseIntOrNull(form.respiratoryRate),
        temperatureCelsius: parseDecimalOrNull(form.temperature),
        weightKg: parseDecimalOrNull(form.weight),
        heightCm: parseDecimalOrNull(form.height),
        oxygenSaturationPercentage: parseIntOrNull(form.spo2),
      },
      diagnosis: form.diagnosis.trim(),
      icd10Code: blankToNull(form.icd10),
      treatmentPlan: blankToNull(form.treatment),
      privateNotes: blankToNull(form.privateNotes),
    };

    setSaving(true);
    try {
      const created = await medicalRecordApi.createConsultation(body);
      toast.success('Consulta guardada', 'La consulta médica fue registrada exitosamente');
      setForm({ ...EMPTY_CONSULTATION, date: toIsoDate(new Date()) });
      onCreated(created);
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No se pudo guardar la consulta médica'));
    } finally {
      setSaving(false);
    }
  };

  const input = (key: keyof typeof EMPTY_CONSULTATION, label: string, placeholder: string, decimal = false) => (
    <Field label={label} htmlFor={`nc-${key}`}>
      <input id={`nc-${key}`} inputMode={decimal ? 'decimal' : 'numeric'} placeholder={placeholder} value={form[key]} onChange={(e) => set(key, e.target.value)} />
    </Field>
  );

  return (
    <form onSubmit={onSubmit} noValidate>
      <fieldset className="form-section" disabled={saving}>
        <legend>Datos de la consulta</legend>
        <div className="form-grid">
          <Field label="Fecha de la consulta" htmlFor="nc-date" required>
            <input id="nc-date" type="date" max={toIsoDate(new Date())} value={form.date} onChange={(e) => set('date', e.target.value)} />
          </Field>
          <Field label="Motivo de la consulta" htmlFor="nc-reason" required>
            <input id="nc-reason" maxLength={255} placeholder="ej. Control rutinario de hipertensión, fiebre de 3 días..." value={form.reason} onChange={(e) => set('reason', e.target.value)} />
          </Field>
          <Field label="Síntomas y evolución" htmlFor="nc-symptoms">
            <textarea id="nc-symptoms" rows={3} placeholder="Descripción detallada de síntomas referidos por el paciente..." value={form.symptoms} onChange={(e) => set('symptoms', e.target.value)} />
          </Field>
          <Field label="Examen físico" htmlFor="nc-physicalExamination">
            <textarea id="nc-physicalExamination" rows={3} placeholder="Hallazgos del examen físico..." value={form.physicalExamination} onChange={(e) => set('physicalExamination', e.target.value)} />
          </Field>
        </div>
      </fieldset>

      <fieldset className="form-section" disabled={saving}>
        <legend>Signos vitales</legend>
        <div className="form-grid form-grid-4">
          {input('systolic', 'Presión sistólica (mmHg)', 'ej. 120')}
          {input('diastolic', 'Presión diastólica (mmHg)', 'ej. 80')}
          {input('heartRate', 'Frecuencia cardíaca (lpm)', 'ej. 75')}
          {input('respiratoryRate', 'Frecuencia respiratoria (rpm)', 'ej. 16')}
          {input('temperature', 'Temperatura (°C)', 'ej. 36.5', true)}
          {input('weight', 'Peso (kg)', 'ej. 70.5', true)}
          {input('height', 'Talla (cm)', 'ej. 172', true)}
          {input('spo2', 'Saturación de oxígeno (%)', 'ej. 98')}
        </div>
        <p className="field-hint">Nota: El IMC se calcula automáticamente en el servidor al procesar peso y talla.</p>
      </fieldset>

      <fieldset className="form-section" disabled={saving}>
        <legend>Diagnóstico y tratamiento</legend>
        <div className="form-grid">
          <Field label="Diagnóstico" htmlFor="nc-diagnosis" required>
            <textarea id="nc-diagnosis" rows={2} placeholder="Diagnóstico principal o presuntivo..." value={form.diagnosis} onChange={(e) => set('diagnosis', e.target.value)} />
          </Field>
          <Field label="Código CIE-10" htmlFor="nc-icd10">
            <input id="nc-icd10" maxLength={20} placeholder="ej. I10" value={form.icd10} onChange={(e) => set('icd10', e.target.value)} />
          </Field>
          <Field label="Plan de tratamiento y receta" htmlFor="nc-treatment">
            <textarea id="nc-treatment" rows={3} placeholder="Indicaciones farmacológicas, estilo de vida, estudios solicitados..." value={form.treatment} onChange={(e) => set('treatment', e.target.value)} />
          </Field>
          <Field label="Notas privadas del médico" htmlFor="nc-privateNotes">
            <textarea id="nc-privateNotes" rows={3} placeholder="Observaciones confidenciales de seguimiento..." value={form.privateNotes} onChange={(e) => set('privateNotes', e.target.value)} />
          </Field>
        </div>
      </fieldset>

      <div className="form-actions">
        <button type="submit" className="btn btn-primary" disabled={saving}>
          {saving ? <Spinner size={16} /> : <FileText size={16} aria-hidden />} Registrar consulta
        </button>
      </div>
    </form>
  );
}

function AppointmentsHistory({ appointments }: { appointments: Appointment[] }) {
  if (appointments.length === 0) {
    return <EmptyState icon={CalendarDays} message="El paciente no tiene citas registradas" />;
  }
  return (
    <div className="table-wrap">
      <table className="table">
        <thead>
          <tr>
            <th>Fecha</th>
            <th>Hora</th>
            <th>Motivo</th>
            <th>Estado</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((a) => (
            <tr key={a.id}>
              <td className="nowrap">{formatDate(a.startTime)}</td>
              <td className="mono nowrap">{formatTime(a.startTime)}</td>
              <td>{a.reasonForVisit ?? '-'}</td>
              <td>
                <AppointmentStatusBadge status={a.status} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
