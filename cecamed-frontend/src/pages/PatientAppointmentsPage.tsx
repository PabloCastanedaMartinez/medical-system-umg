import { useEffect, useState } from 'react';
import { useParams } from 'react-router';
import { patientsApi, portalApi } from '../api/endpoints';
import { useApi } from '../utils/useApi';
import { useAuth } from '../auth/AuthContext';
import { PageHeader, Spinner, ErrorBanner } from '../components/Common';
import { AppointmentStatusBadge } from '../components/StatusBadge';
import { formatDateTime } from '../utils/format';
import { AppointmentFormModal } from './AppointmentFormModal';
import type { Appointment } from '../api/types';

export function PatientAppointmentsPage() {
  const { user } = useAuth();
  const { patientId } = useParams();
  const portal = user?.role === 'PACIENTE';
  const id = Number(patientId);
  const [open, setOpen] = useState(false);
  const history = useApi(() => portal ? portalApi.history() : patientsApi.appointments(id), [portal, id]);
  const profile = useApi(() => portal ? portalApi.profile() : patientsApi.get(id), [portal, id]);
  useEffect(() => { const timer = window.setInterval(history.reload, 60_000); return () => window.clearInterval(timer); }, [history.reload]);
  return <div className="page">
    <PageHeader title={portal ? 'Mis citas' : 'Historial de citas'} subtitle={profile.data?.fullName}
      actions={portal ? <button className="btn btn-primary" onClick={() => setOpen(true)}>Solicitar cita</button> : undefined} />
    {profile.error && <ErrorBanner message={profile.error} onRetry={profile.reload} />}
    {history.error && <ErrorBanner message={history.error} onRetry={history.reload} />}
    <section className="card"><div className="toolbar"><h2>Citas registradas</h2><button className="btn btn-ghost" onClick={history.reload}>Actualizar</button></div>
      {history.loading && <Spinner />}
      <AppointmentHistoryTable appointments={history.data ?? []} />
      {!history.loading && history.data?.length === 0 && <p className="muted">Todavía no hay citas registradas.</p>}
    </section>
    <AppointmentFormModal open={open} mode={{ kind: 'create' }} onClose={() => setOpen(false)} onSaved={() => { setOpen(false); history.reload(); }} />
  </div>;
}
export function AppointmentHistoryTable({ appointments }: { appointments: Appointment[] }) {
  return <div className="table-wrap"><table className="table"><thead><tr><th>Fecha y hora</th><th>Médico</th><th>Especialidad</th><th>Motivo</th><th>Estado</th></tr></thead>
    <tbody>{[...appointments].sort((a, b) => b.startTime.localeCompare(a.startTime)).map((a) => <tr key={a.id}>
      <td>{formatDateTime(a.startTime)}</td><td>{a.doctorName}</td><td>{a.specialty}</td><td>{a.reasonForVisit}</td><td><AppointmentStatusBadge status={a.status} /></td>
    </tr>)}</tbody></table></div>;
}
