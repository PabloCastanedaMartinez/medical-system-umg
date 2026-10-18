import { useEffect, useMemo, useState, type FormEvent } from 'react';
import { CalendarCheck } from 'lucide-react';
import { Modal } from '../components/Modal';
import { Field, Spinner } from '../components/Common';
import { useToast } from '../components/Toast';
import { appointmentsApi, patientsApi, doctorsApi, portalApi } from '../api/endpoints';
import { ApiError, errorMessage } from '../api/client';
import type { Appointment, AvailableSlot, Patient, Doctor } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { APPOINTMENT_STATUS } from '../utils/labels';
import { blankToNull, clinicNow, formatDateTime, formatTime, parseLocalDateTime, startOfDay, toIsoDate } from '../utils/format';

export type AppointmentFormMode =
  | { kind: 'create'; date?: Date; patientId?: number }
  | { kind: 'reschedule'; appointment: Appointment };

interface Props {
  open: boolean;
  mode: AppointmentFormMode;
  onClose: () => void;
  onSaved: (appointment: Appointment) => void;
}

/** Equivalente de appointment-dialog.fxml / AppointmentDialogController (agendar y reprogramar). */
export function AppointmentFormModal({ open, mode, onClose, onSaved }: Props) {
  const toast = useToast();
  const { user } = useAuth();
  const isPatient = user?.role === 'PACIENTE';
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [doctorId, setDoctorId] = useState<number | ''>('');
  const [slotTick, setSlotTick] = useState(0);
  const isReschedule = mode.kind === 'reschedule';

  const [patients, setPatients] = useState<Patient[]>([]);
  const [patientFilter, setPatientFilter] = useState('');
  const [patientId, setPatientId] = useState<number | ''>('');
  const [date, setDate] = useState(toIsoDate(clinicNow()));
  const [slots, setSlots] = useState<AvailableSlot[]>([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [selectedSlot, setSelectedSlot] = useState<AvailableSlot | null>(null);
  const [reason, setReason] = useState('');
  const [notes, setNotes] = useState('');
  const [rescheduleReason, setRescheduleReason] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  // Inicializa el formulario al abrir.
  useEffect(() => {
    if (!open) return;
    setError(null);
    setPatientFilter('');
    setDoctorId(mode.kind === 'reschedule' ? mode.appointment.doctorId : '');
    setSlots([]);
    setSelectedSlot(null);
    let active = true;
    doctorsApi.list().then((list) => {
      if (!active) return;
      setDoctors(list);
      if (mode.kind === 'create') setDoctorId(list[0]?.id ?? '');
    }).catch((err) => active && setError(errorMessage(err)));

    setRescheduleReason('');
    if (mode.kind === 'reschedule') {
      const a = mode.appointment;
      setPatientId(a.patientId);
      setReason(a.reasonForVisit ?? '');
      setNotes(a.notes ?? '');
      setDate(toIsoDate(parseLocalDateTime(a.startTime) ?? clinicNow()));
    } else {
      setPatientId(mode.patientId ?? '');
      setReason('');
      setNotes('');
      const base = mode.date && mode.date >= startOfDay(clinicNow()) ? mode.date : clinicNow();
      setDate(toIsoDate(base));
    }
    (isPatient ? portalApi.profile().then((p) => [p]) : patientsApi.list('', 'ACTIVE'))
      .then((list) => {
        if (!active) return;
        setPatients(list);
        if (mode.kind === 'create' && !mode.patientId && list.length > 0) {
          setPatientId((current) => (current === '' ? list[0].id : current));
        }
      })
      .catch((err) => active && setError(errorMessage(err, 'No fue posible cargar los pacientes')));
    return () => { active = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  // Carga los horarios del día seleccionado.
  useEffect(() => {
    if (!open || !date || doctorId === '') return;
    let cancelled = false;
    setSlots([]);
    setSlotsLoading(true);
    setSelectedSlot(null);
    appointmentsApi
      .availableSlots(date, doctorId, mode.kind === 'reschedule' ? mode.appointment.id : undefined)
      .then((list) => {
        if (cancelled) return;
        setSlots(list);
        const now = clinicNow();
        const first = list.find((s) => s.available && (parseLocalDateTime(s.startTime) ?? now) > now);
        setSelectedSlot(first ?? null);
      })
      .catch((err) => !cancelled && setError(errorMessage(err, 'No fue posible cargar los horarios para la fecha seleccionada')))
      .finally(() => !cancelled && setSlotsLoading(false));
    return () => {
      cancelled = true;
    };
  }, [open, date, doctorId, slotTick]);

  const filteredPatients = useMemo(() => {
    const term = patientFilter.trim().toLowerCase();
    if (!term) return patients;
    return patients.filter(
      (p) => p.fullName.toLowerCase().includes(term) || (p.medicalRecordNumber ?? '').toLowerCase().includes(term) || p.id === patientId,
    );
  }, [patients, patientFilter, patientId]);

  const now = clinicNow();
  const slotState = (s: AvailableSlot): { enabled: boolean; reason: string | null } => {
    const start = parseLocalDateTime(s.startTime);
    if (start && start <= now) return { enabled: false, reason: 'Horario ya transcurrido' };
    if (!s.available) return { enabled: false, reason: s.reasonIfNotAvailable ?? 'No disponible' };
    return { enabled: true, reason: null };
  };

  const validate = (): string | null => {
    if (doctorId === '') return 'Debe seleccionar un médico';
    if (slotsLoading) return 'Espere a que carguen los horarios';
    if (patientId === '') return 'Debe seleccionar un paciente para la cita';
    if (!date) return 'Debe seleccionar una fecha para la cita';
    if (!selectedSlot) return 'Debe seleccionar un horario disponible';
    if (!slotState(selectedSlot).enabled) return 'El horario seleccionado está ocupado o bloqueado. Elija otro horario.';
    if (!reason.trim()) return 'El motivo de la consulta es obligatorio';
    if (isReschedule && !rescheduleReason.trim()) return 'Indique el motivo por el cual se reprograma la cita';
    return null;
  };

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const validation = validate();
    if (validation) {
      setError(validation);
      return;
    }
    const slot = selectedSlot!;
    setSaving(true);
    setError(null);
    try {
      if (mode.kind === 'create') {
        const created = await (isPatient ? portalApi.create : appointmentsApi.create)({
          doctorId: doctorId as number,
          patientId: patientId as number,
          startTime: slot.startTime,
          endTime: slot.endTime,
          reasonForVisit: reason.trim(),
          notes: blankToNull(notes),
        });
        toast.success('Solicitud registrada', `${APPOINTMENT_STATUS[created.status].label}: ${formatDateTime(created.startTime)}`);
        onSaved(created);
      } else {
        const updated = await appointmentsApi.reschedule(mode.appointment.id, {
          version: mode.appointment.version,
          newStartTime: slot.startTime,
          newEndTime: slot.endTime,
          reason: rescheduleReason.trim(),
        });
        toast.success('Cita reprogramada', `La cita fue reprogramada con éxito para el ${formatDateTime(updated.startTime)}`);
        onSaved(updated);
      }
    } catch (err) {
      setError(errorMessage(err, 'No fue posible agendar la cita'));
      if (err instanceof ApiError && err.status === 409) {
        setSlots([]); setSelectedSlot(null); setSlotTick((tick) => tick + 1);
      }
    } finally {
      setSaving(false);
    }
  };

  const title = isReschedule ? 'Reprogramar cita' : 'Nueva cita';
  const subtitle = isReschedule
    ? `Seleccione el nuevo día y horario para el paciente ${mode.appointment.patientFullName ?? ''}`
    : isPatient ? 'Seleccione médico, fecha y horario libre' : 'Seleccione paciente, médico, fecha y horario libre';

  return (
    <Modal
      open={open}
      onClose={onClose}
      locked={saving}
      size="lg"
      title={title}
      subtitle={subtitle}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={saving || slotsLoading}>
            Cancelar
          </button>
          <button type="submit" form="appointment-form" className="btn btn-primary" disabled={saving || slotsLoading}>
            {saving ? <Spinner size={16} /> : <CalendarCheck size={16} aria-hidden />}
            {isReschedule ? 'Reprogramar' : 'Agendar cita'}
          </button>
        </>
      }
    >
      <form id="appointment-form" onSubmit={onSubmit} noValidate>
        <fieldset disabled={saving} className="form-section">
          <Field label="Médico y especialidad" htmlFor="ap-doctor" required>
            <select id="ap-doctor" disabled={isReschedule} value={doctorId}
              onChange={(e) => { setSlots([]); setSelectedSlot(null); setDoctorId(e.target.value ? Number(e.target.value) : ''); }}>
              <option value="">Seleccionar médico...</option>
              {doctors.map((d) => <option key={d.id} value={d.id}>{d.fullName} · {d.specialty}</option>)}
            </select>
          </Field>
          <div className="form-grid">
            <Field label="Paciente" htmlFor="ap-patient" required className="span-2">
              {!isReschedule && !isPatient && (
                <input
                  type="search"
                  className="mb-xs"
                  placeholder="Filtrar por nombre o expediente..."
                  value={patientFilter}
                  onChange={(e) => setPatientFilter(e.target.value)}
                  aria-label="Filtrar pacientes"
                />
              )}
              <select
                id="ap-patient"
                value={patientId}
                disabled={isReschedule || isPatient}
                onChange={(e) => setPatientId(e.target.value ? Number(e.target.value) : '')}
              >
                <option value="">Seleccionar paciente...</option>
                {filteredPatients.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.fullName} (Exp.: {p.medicalRecordNumber ?? '-'})
                  </option>
                ))}
                {isReschedule && !patients.some((p) => p.id === patientId) && (
                  <option value={patientId}>{mode.appointment.patientFullName}</option>
                )}
              </select>
            </Field>

            <Field label="Fecha de la cita" htmlFor="ap-date" required>
              <input id="ap-date" type="date" min={toIsoDate(clinicNow())} value={date} onChange={(e) => setDate(e.target.value)} />
            </Field>

            <Field label="Motivo de la consulta" htmlFor="ap-reason" required>
              <input id="ap-reason" maxLength={255} value={reason} onChange={(e) => setReason(e.target.value)} placeholder="ej. Control de presión arterial" />
            </Field>
          </div>

          <div className="field">
            <span className="field-label" id="slots-label">
              Horario disponible <span className="required">*</span>
            </span>
            {slotsLoading ? (
              <div className="center-pad">
                <Spinner />
              </div>
            ) : slots.length === 0 ? (
              <p className="muted slot-empty">No hay horarios libres para este médico en la fecha seleccionada.</p>
            ) : (
              <div className="slot-grid" role="radiogroup" aria-labelledby="slots-label">
                {slots.map((s) => {
                  const st = slotState(s);
                  const selected = selectedSlot?.startTime === s.startTime;
                  return (
                    <button
                      type="button"
                      key={s.startTime}
                      role="radio"
                      aria-checked={selected}
                      className={`slot${selected ? ' selected' : ''}`}
                      disabled={!st.enabled}
                      title={st.reason ?? 'Disponible'}
                      onClick={() => setSelectedSlot(s)}
                    >
                      {formatTime(s.startTime)} - {formatTime(s.endTime)}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {isReschedule && (
            <Field label="Motivo de la reprogramación" htmlFor="ap-resched" required>
              <input id="ap-resched" maxLength={255} value={rescheduleReason} onChange={(e) => setRescheduleReason(e.target.value)} />
            </Field>
          )}

          {!isReschedule && (
            <Field label="Notas" htmlFor="ap-notes">
              <textarea id="ap-notes" rows={2} value={notes} onChange={(e) => setNotes(e.target.value)} />
            </Field>
          )}
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
