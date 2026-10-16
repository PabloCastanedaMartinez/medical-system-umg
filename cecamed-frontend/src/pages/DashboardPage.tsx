import { Link, useNavigate } from 'react-router';
import { CalendarDays } from 'lucide-react';
import { useAuth } from '../auth/AuthContext';
import { dashboardApi } from '../api/endpoints';
import { useApi } from '../utils/useApi';
import { formatLongDate, formatTime } from '../utils/format';
import { StatCard } from '../components/StatCard';
import { AppointmentStatusBadge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, PageHeader, Spinner } from '../components/Common';

function greeting(date: Date): string {
  const h = date.getHours();
  if (h < 12) return 'Buenos días';
  if (h < 19) return 'Buenas tardes';
  return 'Buenas noches';
}

/** Inicio: resumen del día y citas de hoy (dashboard.fxml / DashboardController). */
export function DashboardPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { data, loading, error, reload } = useApi(() => dashboardApi.summary(), []);
  const today = new Date();

  return (
    <div className="page">
      <PageHeader
        title={`${greeting(today)}, ${user?.fullName ?? ''}`}
        subtitle={formatLongDate(today)}
        actions={
          <>
            <button className="btn btn-secondary" onClick={() => navigate('/pacientes?nuevo=1')}>
              Registrar paciente
            </button>
            <button className="btn btn-primary" onClick={() => navigate('/agenda?nueva=1')}>
              Nueva cita
            </button>
          </>
        }
      />

      {error && <ErrorBanner message={error} onRetry={reload} />}

      <div className="stat-grid" aria-label="Resumen del día">
        <StatCard title="Citas de hoy" value={data?.appointmentsToday ?? null} subtitle="programadas para el día" variant="primary" />
        <StatCard title="En sala de espera" value={data?.waitingRoom ?? null} subtitle="esperando ser atendidos" variant="warning" />
        <StatCard title="Atendidos" value={data?.completedToday ?? null} subtitle="consultas terminadas hoy" variant="success" />
        <StatCard title="Pacientes activos" value={data?.activePatients ?? null} subtitle="en el directorio" variant="accent" />
      </div>

      <section className="card">
        <div className="card-header">
          <h2>Citas de hoy</h2>
          <span className="card-header-links">
            <Link to="/recepcion" className="link">Ir a recepción</Link>
            <Link to="/agenda" className="link">Ver agenda completa</Link>
          </span>
        </div>
        {loading && !data ? (
          <div className="center-pad">
            <Spinner />
          </div>
        ) : data && data.todayAppointments.length > 0 ? (
          <div className="table-wrap">
            <table className="table">
              <thead>
                <tr>
                  <th>Hora</th>
                  <th>Paciente</th>
                  <th>Motivo</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {data.todayAppointments.map((a) => (
                  <tr key={a.id}>
                    <td className="nowrap num">{formatTime(a.startTime)}</td>
                    <td className="strong">{a.patientFullName ?? 'Paciente no asignado'}</td>
                    <td>{a.reasonForVisit ?? '-'}</td>
                    <td>
                      <AppointmentStatusBadge status={a.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState icon={CalendarDays} message="No hay citas para hoy. Puede agendar una con el botón Nueva cita." />
        )}
      </section>
    </div>
  );
}
