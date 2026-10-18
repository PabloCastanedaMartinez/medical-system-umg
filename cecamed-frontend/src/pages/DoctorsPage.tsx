import { useState, type FormEvent } from 'react';
import { doctorsApi, scheduleApi } from '../api/endpoints';
import type { Doctor, DayOfWeek, HoursRequest } from '../api/types';
import { errorMessage } from '../api/client';
import { PageHeader, Field, Spinner, ErrorBanner } from '../components/Common';
import { useApi } from '../utils/useApi';
import { DAY_LABEL, DAY_ORDER } from '../utils/labels';
import { useToast } from '../components/Toast';

const newHours = (): HoursRequest => ({ dayOfWeek: 'MONDAY', startTime: '08:00', endTime: '12:00', slotDurationMinutes: 30 });
export function DoctorsPage() {
  const query = useApi(() => doctorsApi.list(), []);
  const [editing, setEditing] = useState<Doctor | null>(null);
  const [fullName, setFullName] = useState('');
  const [specialty, setSpecialty] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [hours, setHours] = useState<HoursRequest[]>([newHours()]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const toast = useToast();
  const reset = () => { setEditing(null); setFullName(''); setSpecialty(''); setUsername(''); setPassword(''); setHours([newHours()]); setError(null); };
  const edit = async (doctor: Doctor) => {
    setSaving(true); setError(null);
    try {
      const schedules = await scheduleApi.schedules();
      setEditing(doctor); setFullName(doctor.fullName); setSpecialty(doctor.specialty); setUsername(doctor.username); setPassword('');
      setHours(schedules.filter((h) => h.doctorId === doctor.id).map(({ dayOfWeek, startTime, endTime, slotDurationMinutes }) =>
        ({ dayOfWeek, startTime, endTime, slotDurationMinutes: slotDurationMinutes ?? 30 })));
    } catch (err) { setError(errorMessage(err)); } finally { setSaving(false); }
  };
  const save = async (e: FormEvent) => {
    e.preventDefault(); setSaving(true); setError(null);
    try {
      const body = { fullName, specialty, username, password: password || undefined, schedules: hours, version: editing?.version };
      if (editing) await doctorsApi.update(editing.id, body); else await doctorsApi.create(body);
      toast.success('Médico guardado', 'Especialidad y jornada de atención actualizadas'); reset(); query.reload();
    } catch (err) { setError(errorMessage(err)); } finally { setSaving(false); }
  };
  const changeHours = (index: number, patch: Partial<HoursRequest>) => setHours((all) => all.map((h, i) => i === index ? { ...h, ...patch } : h));
  return <div className="page">
    <PageHeader title="Médicos y horarios" subtitle="Configure la especialidad y los bloques semanales de cada médico" />
    {query.error && <ErrorBanner message={query.error} onRetry={query.reload} />}
    <section className="card">
      <h2>Médicos registrados</h2>
      {query.loading && <Spinner />}
      <div className="table-wrap"><table className="table"><thead><tr><th>Médico</th><th>Especialidad</th><th>Usuario</th><th>Acción</th></tr></thead>
        <tbody>{(query.data ?? []).map((d) => <tr key={d.id}><td>{d.fullName}</td><td>{d.specialty}</td><td>{d.username}</td>
          <td><button className="btn btn-ghost btn-sm" disabled={saving} onClick={() => void edit(d)}>Editar horario</button></td></tr>)}</tbody></table></div>
      {!query.loading && query.data?.length === 0 && <p className="muted">Registre el primer médico para habilitar las citas.</p>}
    </section>
    <section className="card"><h2>{editing ? `Editar a ${editing.fullName}` : 'Registrar médico'}</h2>
      <form onSubmit={save}><fieldset className="form-section" disabled={saving}>
        <div className="form-grid">
          <Field label="Nombre completo" htmlFor="doctor-name" required><input id="doctor-name" required maxLength={150} value={fullName} onChange={(e) => setFullName(e.target.value)} /></Field>
          <Field label="Especialidad" htmlFor="doctor-specialty" required><input id="doctor-specialty" required maxLength={150} value={specialty} onChange={(e) => setSpecialty(e.target.value)} /></Field>
          <Field label="Usuario de acceso" htmlFor="doctor-user" required><input id="doctor-user" required minLength={3} maxLength={80} value={username} onChange={(e) => setUsername(e.target.value)} /></Field>
          {!editing && <Field label="Contraseña para cuenta nueva" htmlFor="doctor-password"><input id="doctor-password" type="password" autoComplete="new-password" minLength={12} maxLength={100} value={password} onChange={(e) => setPassword(e.target.value)} /><small>Si ya existe una cuenta médica, deje la contraseña vacía.</small></Field>}
        </div>
        <h3>Bloques de atención</h3>
        {hours.map((h, i) => <div className="form-grid" key={i}>
          <Field label="Día" htmlFor={`hours-day-${i}`}><select id={`hours-day-${i}`} value={h.dayOfWeek} onChange={(e) => changeHours(i, { dayOfWeek: e.target.value as DayOfWeek })}>{DAY_ORDER.map((d) => <option key={d} value={d}>{DAY_LABEL[d]}</option>)}</select></Field>
          <Field label="Desde" htmlFor={`hours-start-${i}`}><input id={`hours-start-${i}`} type="time" required value={h.startTime} onChange={(e) => changeHours(i, { startTime: e.target.value })} /></Field>
          <Field label="Hasta" htmlFor={`hours-end-${i}`}><input id={`hours-end-${i}`} type="time" required value={h.endTime} onChange={(e) => changeHours(i, { endTime: e.target.value })} /></Field>
          <Field label="Minutos por cita" htmlFor={`hours-duration-${i}`}><input id={`hours-duration-${i}`} type="number" min={5} max={240} required value={h.slotDurationMinutes} onChange={(e) => changeHours(i, { slotDurationMinutes: Number(e.target.value) })} /></Field>
          <button type="button" className="btn btn-ghost" disabled={hours.length === 1} onClick={() => setHours((all) => all.filter((_, index) => index !== i))}>Quitar bloque</button>
        </div>)}
        <div className="toolbar"><button type="button" className="btn btn-ghost" onClick={() => setHours((all) => [...all, newHours()])}>Añadir bloque</button>
          <button className="btn btn-primary" type="submit">{saving && <Spinner size={16} />} Guardar médico</button>
          {editing && <button type="button" className="btn btn-ghost" onClick={reset}>Cancelar edición</button>}</div>
      </fieldset>{error && <div className="alert alert-danger" role="alert">{error}</div>}</form>
    </section>
  </div>;
}
