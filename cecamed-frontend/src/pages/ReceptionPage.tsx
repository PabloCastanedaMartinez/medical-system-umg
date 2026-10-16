import { useEffect, useMemo, useState } from 'react';
import { ArrowRightCircle, CalendarDays, CalendarPlus, Check, CheckCircle2, Clock, RefreshCw, Search, ThumbsUp, UserCheck, UserX } from 'lucide-react';
import { appointmentsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Appointment, AppointmentStatus } from '../api/types';
import { useToast } from '../components/Toast';
import { useConfirm } from '../components/ConfirmDialog';
import { StatCard } from '../components/StatCard';
import { AppointmentStatusBadge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, PageHeader, Spinner } from '../components/Common';
import { useApi, useNow } from '../utils/useApi';
import { endOfDay, formatElapsed, formatTime, parseLocalDateTime, startOfDay, toIsoDateTime } from '../utils/format';
import { APPOINTMENT_STATUS, isClosedForReception } from '../utils/labels';
import { AppointmentFormModal, type AppointmentFormMode } from './AppointmentFormModal';

const FILTERS: AppointmentStatus[] = ['PROGRAMADA', 'CONFIRMADA', 'EN_SALA', 'ATENDIDA', 'NO_ASISTIO', 'CANCELADA'];

/**
 * Hora de llegada a sala: el backend actualiza `updatedAt` al hacer check-in (estado EN_SALA),
 * así el tiempo de espera es el mismo en todos los equipos de recepción.
 */
function arrivalTime(a: Appointment): Date {
  return parseLocalDateTime(a.updatedAt) ?? parseLocalDateTime(a.startTime) ?? new Date();
}

/** Equivalente de reception.fxml / ReceptionController. */
export function ReceptionPage() {
  const toast = useToast();
  const { confirm } = useConfirm();
  const now = useNow(1000);
  const [search, setSearch] = useState('');
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | ''>('');
  const [busyId, setBusyId] = useState<number | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<AppointmentFormMode>({ kind: 'create' });

  const today = useMemo(() => new Date(), []);
  const { data, loading, error, reload } = useApi(
    (signal) => appointmentsApi.list(toIsoDateTime(startOfDay(today)), toIsoDateTime(endOfDay(today)), null, signal),
    [],
  );
  const all = data ?? [];

  // Refresco automático cada minuto para ver cambios hechos desde otros equipos.
  useEffect(() => {
    const id = window.setInterval(reload, 60_000);
    return () => window.clearInterval(id);
  }, [reload]);

  const metrics = useMemo(
    () => ({
      total: all.length,
      waiting: all.filter((a) => a.status === 'EN_SALA').length,
      done: all.filter((a) => a.status === 'ATENDIDA').length,
      pending: all.filter((a) => a.status === 'PROGRAMADA' || a.status === 'CONFIRMADA').length,
    }),
    [all],
  );

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return all.filter((a) => {
      if (statusFilter && a.status !== statusFilter) return false;
      if (term && !(a.patientFullName ?? '').toLowerCase().includes(term)) return false;
      return true;
    });
  }, [all, search, statusFilter]);

  const waiting = useMemo(
    () => all.filter((a) => a.status === 'EN_SALA').sort((a, b) => arrivalTime(a).getTime() - arrivalTime(b).getTime()),
    [all],
  );

  const updateStatus = async (a: Appointment, status: AppointmentStatus, successMsg: string) => {
    setBusyId(a.id);
    try {
      await appointmentsApi.updateStatus(a.id, status);
      toast.success('Recepción', successMsg);
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible actualizar el estado de la cita'));
    } finally {
      setBusyId(null);
    }
  };

  const noShow = async (a: Appointment) => {
    const ok = await confirm({
      title: 'Marcar Inasistencia',
      message: (
        <>
          <p>
            <strong>¿Desea registrar al paciente como NO ASISTIÓ?</strong>
          </p>
          <p className="muted">
            Paciente: {a.patientFullName} - Cita: {formatTime(a.startTime)}
          </p>
        </>
      ),
      confirmLabel: 'Registrar inasistencia',
      tone: 'danger',
    });
    if (ok) await updateStatus(a, 'NO_ASISTIO', 'Cita marcada como No Asistió');
  };

  return (
    <div className="page">
      <PageHeader
        title="Recepción y Sala de Espera"
        subtitle="Control ágil de flujo de pacientes, check-in y tiempos de espera en tiempo real"
        actions={
          <>
            <button className="btn btn-ghost" onClick={reload} disabled={loading}>
              <RefreshCw size={16} aria-hidden /> Actualizar
            </button>
            <button
              className="btn btn-primary"
              onClick={() => {
                setFormMode({ kind: 'create', date: new Date() });
                setFormOpen(true);
              }}
            >
              <CalendarPlus size={16} aria-hidden /> Cita Rápida
            </button>
          </>
        }
      />

      {error && <ErrorBanner message={error} onRetry={reload} />}

      <div className="stat-grid">
        <StatCard title="Citas de Hoy" value={data ? metrics.total : null} subtitle="Total agendadas" icon={CalendarDays} variant="primary" />
        <StatCard title="En Sala de Espera" value={data ? metrics.waiting : null} subtitle="Pacientes aguardando" icon={Clock} variant="warning" />
        <StatCard title="Atendidos Hoy" value={data ? metrics.done : null} subtitle="Consultas finalizadas" icon={CheckCircle2} variant="success" />
        <StatCard title="Pendientes" value={data ? metrics.pending : null} subtitle="Por ingresar" icon={UserCheck} variant="accent" />
      </div>

      <div className="reception-grid">
        <section className="card">
          <div className="card-header">
            <h2>Citas Médicas de Hoy</h2>
            {loading && <Spinner size={14} />}
          </div>
          <div className="toolbar">
            <div className="search-box">
              <Search size={16} aria-hidden />
              <input type="search" placeholder="Buscar paciente..." value={search} onChange={(e) => setSearch(e.target.value)} aria-label="Buscar paciente" />
            </div>
            <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as AppointmentStatus | '')} aria-label="Filtrar por estado">
              <option value="">Todas</option>
              {FILTERS.map((s) => (
                <option key={s} value={s}>
                  {APPOINTMENT_STATUS[s].label}
                </option>
              ))}
            </select>
            <div className="toolbar-spacer" />
            <span className="muted">Citas listadas: {filtered.length}</span>
          </div>

          {filtered.length === 0 && !loading ? (
            <EmptyState icon={CalendarDays} message="No hay citas que coincidan para el día de hoy" />
          ) : (
            <div className="table-wrap">
              <table className="table">
                <thead>
                  <tr>
                    <th>Hora</th>
                    <th>Paciente</th>
                    <th>Teléfono</th>
                    <th>Motivo</th>
                    <th>Estado</th>
                    <th className="col-actions">Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((a) => {
                    const closed = isClosedForReception(a.status);
                    const busy = busyId === a.id;
                    return (
                      <tr key={a.id}>
                        <td className="mono nowrap">{formatTime(a.startTime)}</td>
                        <td className="strong">{a.patientFullName ?? 'Paciente no asignado'}</td>
                        <td className="nowrap">{a.patientPhone || '-'}</td>
                        <td>{a.reasonForVisit ?? '-'}</td>
                        <td>
                          <AppointmentStatusBadge status={a.status} />
                        </td>
                        <td className="col-actions">
                          <div className="row-actions">
                            {a.status === 'PROGRAMADA' && (
                              <button className="icon-button" title="Confirmar Asistencia" aria-label="Confirmar Asistencia" disabled={busy} onClick={() => updateStatus(a, 'CONFIRMADA', 'Cita confirmada')}>
                                <ThumbsUp size={15} />
                              </button>
                            )}
                            {(a.status === 'PROGRAMADA' || a.status === 'CONFIRMADA' || a.status === 'REPROGRAMADA') && (
                              <button
                                className="btn btn-accent btn-xs"
                                disabled={busy}
                                onClick={() => updateStatus(a, 'EN_SALA', `Check-in completado: ${a.patientFullName} pasó a sala de espera`)}
                              >
                                <UserCheck size={13} aria-hidden /> Check-in
                              </button>
                            )}
                            {a.status === 'EN_SALA' && (
                              <button
                                className="btn btn-primary btn-xs"
                                disabled={busy}
                                onClick={() => updateStatus(a, 'ATENDIDA', 'Paciente ingresado a consulta con el médico')}
                              >
                                <ArrowRightCircle size={13} aria-hidden /> A Consulta
                              </button>
                            )}
                            {!closed && (
                              <>
                                <button className="icon-button text-danger" title="Marcar No Asistió" aria-label="Marcar No Asistió" disabled={busy} onClick={() => noShow(a)}>
                                  <UserX size={15} />
                                </button>
                                <button
                                  className="icon-button"
                                  title="Reprogramar Cita"
                                  aria-label="Reprogramar Cita"
                                  disabled={busy}
                                  onClick={() => {
                                    setFormMode({ kind: 'reschedule', appointment: a });
                                    setFormOpen(true);
                                  }}
                                >
                                  <CalendarDays size={15} />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          )}
        </section>

        <section className="card waiting-room">
          <div className="card-header">
            <h2>Monitor de Sala de Espera</h2>
            <span className="muted">Pacientes en espera: {waiting.length}</span>
          </div>
          {waiting.length === 0 ? (
            <EmptyState icon={Clock} message="No hay pacientes en sala de espera" />
          ) : (
            <ul className="waiting-list">
              {waiting.map((a) => {
                const arrival = arrivalTime(a);
                const minutes = (now.getTime() - arrival.getTime()) / 60000;
                return (
                  <li key={a.id} className={minutes >= 30 ? 'long-wait' : minutes >= 15 ? 'mid-wait' : ''}>
                    <div className="waiting-info">
                      <strong>{a.patientFullName}</strong>
                      <span className="muted small">
                        Cita {formatTime(a.startTime)} · Llegada {formatTime(arrival)}
                      </span>
                    </div>
                    <span className="waiting-elapsed mono" aria-label="Tiempo de espera">
                      {formatElapsed(arrival, now)}
                    </span>
                    <button
                      className="btn btn-primary btn-xs"
                      disabled={busyId === a.id}
                      onClick={() => updateStatus(a, 'ATENDIDA', `Paciente ${a.patientFullName} pasó al consultorio médico`)}
                    >
                      <Check size={13} aria-hidden /> Pasar a Consulta
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
        </section>
      </div>

      <AppointmentFormModal
        open={formOpen}
        mode={formMode}
        onClose={() => setFormOpen(false)}
        onSaved={() => {
          setFormOpen(false);
          reload();
        }}
      />
    </div>
  );
}
