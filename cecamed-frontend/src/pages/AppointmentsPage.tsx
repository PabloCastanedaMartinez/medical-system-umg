import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import { CalendarDays, CalendarPlus, CheckCircle2, ChevronLeft, ChevronRight, ClipboardList, RefreshCw, XCircle } from 'lucide-react';
import { appointmentsApi } from '../api/endpoints';
import { errorMessage } from '../api/client';
import type { Appointment, AppointmentStatus } from '../api/types';
import { useAuth } from '../auth/AuthContext';
import { useToast } from '../components/Toast';
import { useConfirm } from '../components/ConfirmDialog';
import { AppointmentStatusBadge, SyncStatusBadge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, PageHeader, Spinner } from '../components/Common';
import { useApi } from '../utils/useApi';
import {
  addDays,
  addMonths,
  clinicNow,
  endOfDay,
  formatDate,
  formatDayMonth,
  formatLongDate,
  formatMonthYear,
  formatTime,
  parseLocalDateTime,
  startOfDay,
  startOfWeek,
  toIsoDate,
  toIsoDateTime,
} from '../utils/format';
import { APPOINTMENT_STATUS, APPOINTMENT_STATUS_ORDER, isFinalized } from '../utils/labels';
import { AppointmentFormModal, type AppointmentFormMode } from './AppointmentFormModal';

type ViewMode = 'DAY' | 'WEEK' | 'MONTH';

function rangeFor(mode: ViewMode, date: Date): [Date, Date] {
  switch (mode) {
    case 'WEEK': {
      const start = startOfWeek(date);
      return [start, endOfDay(addDays(start, 6))];
    }
    case 'MONTH':
      return [new Date(date.getFullYear(), date.getMonth(), 1), endOfDay(new Date(date.getFullYear(), date.getMonth() + 1, 0))];
    default:
      return [startOfDay(date), endOfDay(date)];
  }
}

function periodLabel(mode: ViewMode, date: Date): string {
  if (mode === 'DAY') return formatLongDate(date);
  if (mode === 'MONTH') return formatMonthYear(date);
  const [start, end] = rangeFor('WEEK', date);
  return `Semana del ${formatDayMonth(start)} al ${formatDayMonth(end)} de ${end.getFullYear()}`;
}

/** Equivalente de appointment-calendar.fxml / AppointmentCalendarController. */
export function AppointmentsPage() {
  const { isRecepcion } = useAuth();
  const toast = useToast();
  const { confirm, prompt } = useConfirm();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();

  const [mode, setMode] = useState<ViewMode>('DAY');
  const [current, setCurrent] = useState(() => clinicNow());
  const [statusFilter, setStatusFilter] = useState<AppointmentStatus | ''>('');
  const [formOpen, setFormOpen] = useState(false);
  const [formMode, setFormMode] = useState<AppointmentFormMode>({ kind: 'create' });

  const [rangeStart, rangeEnd] = useMemo(() => rangeFor(mode, current), [mode, current]);
  const { data, loading, error, reload } = useApi(
    (signal) => appointmentsApi.list(toIsoDateTime(rangeStart), toIsoDateTime(rangeEnd), statusFilter || null, signal),
    [rangeStart.getTime(), rangeEnd.getTime(), statusFilter],
  );
  const appointments = data ?? [];

  useEffect(() => {
    if (params.get('nueva') === '1') {
      setFormMode({ kind: 'create', date: clinicNow() });
      setFormOpen(true);
      params.delete('nueva');
      setParams(params, { replace: true });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const shift = (direction: 1 | -1) => {
    setCurrent((d) => (mode === 'DAY' ? addDays(d, direction) : mode === 'WEEK' ? addDays(d, 7 * direction) : addMonths(d, direction)));
  };

  const openRecord = (a: Appointment) => {
    if (isRecepcion) {
      toast.warning('Acceso restringido', 'El perfil de Recepción no tiene permisos para acceder al expediente clínico detallado');
      return;
    }
    navigate(`/expediente/${a.patientId}`);
  };

  const complete = async (a: Appointment) => {
    const ok = await confirm({
      title: 'Marcar como atendida',
      message: (
        <>
          <p>
            <strong>¿Desea marcar como ATENDIDA la cita?</strong>
          </p>
          <p className="muted">
            Paciente: {a.patientFullName}
            <br />
            Hora: {formatTime(a.startTime)}
          </p>
        </>
      ),
      confirmLabel: 'Marcar como atendida',
    });
    if (!ok) return;
    try {
      await appointmentsApi.complete(a.id);
      toast.success('Cita atendida', 'La cita fue marcada como ATENDIDA exitosamente');
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No se pudo marcar la cita como atendida'));
    }
  };

  const cancel = async (a: Appointment) => {
    const reason = await prompt({
      title: 'Cancelar cita',
      message: <p>Cancelación de cita para <strong>{a.patientFullName}</strong> ({formatDate(a.startTime)} {formatTime(a.startTime)})</p>,
      inputLabel: 'Motivo de la cancelación',
      confirmLabel: 'Cancelar cita',
      cancelLabel: 'Volver',
      tone: 'danger',
    });
    if (!reason) return;
    try {
      await appointmentsApi.cancel(a.id, reason);
      toast.success('Cita cancelada', 'La cita ha sido cancelada correctamente');
      reload();
    } catch (err) {
      toast.error('Error', `No fue posible cancelar la cita: ${errorMessage(err)}`);
    }
  };

  return (
    <div className="page">
      <PageHeader
        title="Agenda"
        actions={
          <button
            className="btn btn-primary"
            onClick={() => {
              setFormMode({ kind: 'create', date: current });
              setFormOpen(true);
            }}
          >
            <CalendarPlus size={16} aria-hidden /> Nueva cita
          </button>
        }
      />

      <div className="card">
        <div className="toolbar toolbar-wrap">
          <div className="btn-group" role="group" aria-label="Navegar período">
            <button className="btn btn-ghost btn-sm" onClick={() => shift(-1)} aria-label="Anterior">
              <ChevronLeft size={16} />
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => setCurrent(clinicNow())}>
              Hoy
            </button>
            <button className="btn btn-ghost btn-sm" onClick={() => shift(1)} aria-label="Siguiente">
              <ChevronRight size={16} />
            </button>
          </div>
          <h2 className="period-label">{periodLabel(mode, current)}</h2>
          <div className="toolbar-spacer" />
          <input
            type="date"
            value={toIsoDate(current)}
            onChange={(e) => e.target.value && setCurrent(parseLocalDateTime(e.target.value) ?? clinicNow())}
            aria-label="Ir a fecha"
          />
          <div className="segmented" role="tablist" aria-label="Vista">
            {(['DAY', 'WEEK', 'MONTH'] as ViewMode[]).map((m) => (
              <button key={m} role="tab" aria-selected={mode === m} className={mode === m ? 'active' : ''} onClick={() => setMode(m)}>
                {m === 'DAY' ? 'Día' : m === 'WEEK' ? 'Semana' : 'Mes'}
              </button>
            ))}
          </div>
          <select value={statusFilter} onChange={(e) => setStatusFilter(e.target.value as AppointmentStatus | '')} aria-label="Filtrar por estado">
            <option value="">Todos los estados</option>
            {APPOINTMENT_STATUS_ORDER.map((s) => (
              <option key={s} value={s}>
                {APPOINTMENT_STATUS[s].label}
              </option>
            ))}
          </select>
        </div>
        <div className="toolbar toolbar-sub">
          <span className="muted">
            Total: {appointments.length} cita{appointments.length === 1 ? '' : 's'}
          </span>
          {loading && <Spinner size={14} />}
          <div className="toolbar-spacer" />
          <button className="btn btn-ghost btn-sm" onClick={reload}>
            <RefreshCw size={14} aria-hidden /> Actualizar
          </button>
        </div>

        {error && <ErrorBanner message={error} onRetry={reload} />}

        {appointments.length === 0 && !loading ? (
          <EmptyState icon={CalendarDays} message="No hay citas médicas programadas para el período seleccionado" />
        ) : (
          <div className="table-wrap">
            <table className="table table-hover">
              <thead>
                <tr>
                  <th>Fecha</th>
                  <th>Hora</th>
                  <th>Paciente</th>
                  <th>Teléfono</th>
                  <th>Motivo</th>
                  <th>Estado</th>
                  <th>Google</th>
                  <th className="col-actions">Acciones</th>
                </tr>
              </thead>
              <tbody>
                {appointments.map((a) => {
                  const done = isFinalized(a.status);
                  return (
                    <tr key={a.id}>
                      <td className="nowrap">{formatDate(a.startTime)}</td>
                      <td className="nowrap mono">
                        {formatTime(a.startTime)} - {formatTime(a.endTime)}
                      </td>
                      <td className="strong">{a.patientFullName ?? 'Paciente no asignado'}</td>
                      <td className="nowrap">{a.patientPhone || '-'}</td>
                      <td>
                        {a.reasonForVisit ?? '-'}
                        {a.status === 'CANCELADA' && a.cancellationReason && (
                          <div className="muted small">Cancelada: {a.cancellationReason}</div>
                        )}
                      </td>
                      <td>
                        <AppointmentStatusBadge status={a.status} />
                      </td>
                      <td>
                        <SyncStatusBadge status={a.googleSyncStatus} />
                      </td>
                      <td className="col-actions">
                        <div className="row-actions">
                          {!isRecepcion && (
                            <button className="icon-button" title="Ver expediente clínico" aria-label="Ver expediente clínico" onClick={() => openRecord(a)}>
                              <ClipboardList size={16} />
                            </button>
                          )}
                          <button
                            className="icon-button"
                            title="Reprogramar cita"
                            aria-label="Reprogramar cita"
                            disabled={done}
                            onClick={() => {
                              setFormMode({ kind: 'reschedule', appointment: a });
                              setFormOpen(true);
                            }}
                          >
                            <RefreshCw size={16} />
                          </button>
                          <button className="icon-button text-success" title="Marcar como atendida" aria-label="Marcar como atendida" disabled={done} onClick={() => complete(a)}>
                            <CheckCircle2 size={16} />
                          </button>
                          <button className="icon-button text-danger" title="Cancelar cita" aria-label="Cancelar cita" disabled={done} onClick={() => cancel(a)}>
                            <XCircle size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      <AppointmentFormModal
        open={formOpen}
        mode={formMode}
        onClose={() => setFormOpen(false)}
        onSaved={(a) => {
          setFormOpen(false);
          const d = parseLocalDateTime(a.startTime);
          if (d) setCurrent(d);
          reload();
        }}
      />
    </div>
  );
}
