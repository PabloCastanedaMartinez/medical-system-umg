import { useState, type FormEvent } from 'react';
import { CalendarX, Moon, Plus, Sun, Trash2 } from 'lucide-react';
import { apiBaseUrl, scheduleApi, systemApi, doctorsApi } from '../api/endpoints';
import { useAuth } from '../auth/AuthContext';
import { errorMessage } from '../api/client';
import type { BlockType, ScheduleBlock } from '../api/types';
import { useTheme, type ThemeMode } from '../theme/ThemeContext';
import { useToast } from '../components/Toast';
import { useConfirm } from '../components/ConfirmDialog';
import { Badge } from '../components/StatusBadge';
import { EmptyState, ErrorBanner, Field, PageHeader, Spinner } from '../components/Common';
import { useApi } from '../utils/useApi';
import { clinicNow, combineDateTime, formatDateTime, formatLocalTime, toIsoDate } from '../utils/format';
import { BLOCK_TYPE_LABEL, DAY_LABEL, DAY_ORDER } from '../utils/labels';

type Tab = 'appearance' | 'schedules' | 'blocks' | 'system';

const TABS: { id: Tab; label: string }[] = [
  { id: 'appearance', label: 'Apariencia' },
  { id: 'schedules', label: 'Horarios de atención' },
  { id: 'blocks', label: 'Bloqueos de agenda' },
  { id: 'system', label: 'Sistema' },
];

/** Equivalente de settings.fxml / SettingsController. */
export function SettingsPage() {
  const [tab, setTab] = useState<Tab>('appearance');
  const { user } = useAuth();
  return (
    <div className="page">
      <PageHeader
        title="Configuración"
        subtitle="Apariencia, horarios de atención y bloqueos de agenda"
      />
      <div className="tabs" role="tablist">
        {TABS.filter((t) => ['RECEPCION', 'ADMIN'].includes(user?.role ?? '') || t.id !== 'blocks').map((t) => (
          <button key={t.id} role="tab" aria-selected={tab === t.id} className={tab === t.id ? 'active' : ''} onClick={() => setTab(t.id)}>
            {t.label}
          </button>
        ))}
      </div>
      <section className="card tab-panel" role="tabpanel">
        {tab === 'appearance' && <AppearanceTab />}
        {tab === 'schedules' && <SchedulesTab />}
        {tab === 'blocks' && <BlocksTab />}
        {tab === 'system' && <SystemTab />}
      </section>
    </div>
  );
}

function AppearanceTab() {
  const { theme, setTheme } = useTheme();
  const toast = useToast();
  const choose = (t: ThemeMode) => {
    if (t === theme) return;
    setTheme(t);
    toast.info('Tema actualizado', `Ahora se usa el tema ${t === 'dark' ? 'oscuro' : 'claro'}`);
  };
  return (
    <>
      <h3 className="section-title">Tema</h3>
      <p className="muted">Cambie entre tema claro y oscuro en tiempo real. La preferencia se guarda en este navegador.</p>
      <div className="theme-options" role="radiogroup" aria-label="Tema">
        <button role="radio" aria-checked={theme === 'light'} className={`theme-option${theme === 'light' ? ' selected' : ''}`} onClick={() => choose('light')}>
          <Sun size={22} aria-hidden />
          <strong>Claro</strong>
          <span>Fondo blanco y pizarra claro optimizado para consultorios con luz natural.</span>
        </button>
        <button role="radio" aria-checked={theme === 'dark'} className={`theme-option${theme === 'dark' ? ' selected' : ''}`} onClick={() => choose('dark')}>
          <Moon size={22} aria-hidden />
          <strong>Oscuro</strong>
          <span>Fondo pizarra oscuro profesional que reduce la fatiga visual nocturna.</span>
        </button>
      </div>
      <h3 className="section-title">Colores del sistema</h3>
      <div className="swatches">
        <div><span className="swatch swatch-primary" />Rojo médico: marca y acciones principales</div>
        <div><span className="swatch swatch-accent" />Celeste médico: acciones secundarias y selección activa</div>
        <div><span className="swatch swatch-success" />Verde: atención finalizada y operaciones exitosas</div>
      </div>
    </>
  );
}

function SchedulesTab() {
  const { data, loading, error, reload } = useApi(() => scheduleApi.schedules(), []);
  const schedules = [...(data ?? [])].sort((a, b) => DAY_ORDER.indexOf(a.dayOfWeek) - DAY_ORDER.indexOf(b.dayOfWeek));
  return (
    <>
      <h3 className="section-title">Horarios de consulta</h3>
      {error && <ErrorBanner message={error} onRetry={reload} />}
      {loading && !data ? (
        <div className="center-pad"><Spinner /></div>
      ) : schedules.length === 0 ? (
        <EmptyState icon={CalendarX} message="No hay horarios de atención configurados" />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Médico</th>
                <th>Día</th>
                <th>Inicio</th>
                <th>Cierre</th>
                <th>Duración por cita</th>
                <th>Estado</th>
              </tr>
            </thead>
            <tbody>
              {schedules.map((s) => (
                <tr key={s.id ?? s.dayOfWeek}>
                  <td>{s.doctorName}</td>
                  <td className="strong">{DAY_LABEL[s.dayOfWeek]}</td>
                  <td className="mono">{formatLocalTime(s.startTime)}</td>
                  <td className="mono">{formatLocalTime(s.endTime)}</td>
                  <td>{s.slotDurationMinutes != null ? `${s.slotDurationMinutes} min` : '--'}</td>
                  <td>
                    <Badge tone={s.isActive ? 'success' : 'neutral'}>{s.isActive ? 'Laborable' : 'No laborable'}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function BlocksTab() {
  const doctors = useApi(() => doctorsApi.list(), []);
  const [doctorId, setDoctorId] = useState<number | ''>('');
  const toast = useToast();
  const { confirm } = useConfirm();
  const today = toIsoDate(clinicNow());
  const { data, loading, error, reload } = useApi(() => scheduleApi.blocks(), []);
  const [form, setForm] = useState({
    title: '',
    blockType: 'PERSONAL' as BlockType,
    startDate: today,
    startTime: '12:00',
    endDate: today,
    endTime: '13:00',
    reason: '',
  });
  const [saving, setSaving] = useState(false);
  const set = (key: keyof typeof form, value: string) => setForm((f) => ({ ...f, [key]: value }));

  const onCreate = async (e: FormEvent) => {
    e.preventDefault();
    if (doctorId === '') { toast.warning('Seleccione médico', 'Indique a quién corresponde el bloqueo'); return; }
    if (!form.title.trim()) {
      toast.warning('Campo obligatorio', 'Ingrese un título descriptivo para el bloqueo');
      return;
    }
    if (!form.startDate || !form.endDate) {
      toast.warning('Faltan las fechas', 'Seleccione fecha de inicio y fin');
      return;
    }
    const start = combineDateTime(form.startDate, form.startTime || '08:00');
    const end = combineDateTime(form.endDate, form.endTime || '17:00');
    if (start >= end) {
      toast.warning('Rango inválido', 'La fecha y hora de inicio debe ser anterior a la de fin');
      return;
    }
    setSaving(true);
    try {
      await scheduleApi.createBlock({
        doctorId,
        title: form.title.trim(),
        blockType: form.blockType,
        startDateTime: start,
        endDateTime: end,
        allDay: false,
        reason: form.reason.trim() || null,
      });
      toast.success('Bloqueo creado', 'El bloqueo de agenda fue registrado con éxito');
      setForm((f) => ({ ...f, title: '', reason: '' }));
      reload();
    } catch (err) {
      toast.error('Error', `No se pudo crear el bloqueo: ${errorMessage(err)}`);
    } finally {
      setSaving(false);
    }
  };

  const onDelete = async (b: ScheduleBlock) => {
    const ok = await confirm({
      title: 'Eliminar bloqueo',
      message: <p>¿Desea eliminar este bloqueo de agenda?<br /><span className="muted">Bloqueo: {b.title} ({BLOCK_TYPE_LABEL[b.blockType]})</span></p>,
      confirmLabel: 'Eliminar',
      tone: 'danger',
    });
    if (!ok) return;
    try {
      await scheduleApi.deleteBlock(b.id);
      toast.success('Bloqueo eliminado', 'El bloqueo fue removido correctamente');
      reload();
    } catch (err) {
      toast.error('Error', errorMessage(err, 'No fue posible eliminar el bloqueo'));
    }
  };

  return (
    <>
      <h3 className="section-title">Nuevo bloqueo (descansos, vacaciones o actividades)</h3>
      <form onSubmit={onCreate} noValidate>
        <fieldset className="form-section" disabled={saving}>
          <Field label="Médico" htmlFor="block-doctor" required><select id="block-doctor" value={doctorId} onChange={(e) => setDoctorId(e.target.value ? Number(e.target.value) : '')}>
            <option value="">Seleccione médico...</option>{doctors.data?.map((d) => <option key={d.id} value={d.id}>{d.fullName}</option>)}
          </select></Field>
          {doctors.error && <ErrorBanner message={doctors.error} onRetry={doctors.reload} />}
          <div className="form-grid form-grid-3">
            <Field label="Motivo" htmlFor="bl-title" required>
              <input id="bl-title" maxLength={150} value={form.title} onChange={(e) => set('title', e.target.value)} placeholder="ej. Almuerzo, Congreso médico" />
            </Field>
            <Field label="Tipo de bloqueo" htmlFor="bl-type" required>
              <select id="bl-type" value={form.blockType} onChange={(e) => set('blockType', e.target.value)}>
                {(Object.keys(BLOCK_TYPE_LABEL) as BlockType[]).map((t) => (
                  <option key={t} value={t}>{BLOCK_TYPE_LABEL[t]}</option>
                ))}
              </select>
            </Field>
            <Field label="Notas" htmlFor="bl-reason">
              <input id="bl-reason" value={form.reason} onChange={(e) => set('reason', e.target.value)} />
            </Field>
            <Field label="Inicio (fecha y hora)" htmlFor="bl-start-date" required>
              <div className="input-pair">
                <input id="bl-start-date" type="date" value={form.startDate} onChange={(e) => set('startDate', e.target.value)} />
                <input type="time" aria-label="Hora de inicio" value={form.startTime} onChange={(e) => set('startTime', e.target.value)} />
              </div>
            </Field>
            <Field label="Fin (fecha y hora)" htmlFor="bl-end-date" required>
              <div className="input-pair">
                <input id="bl-end-date" type="date" value={form.endDate} onChange={(e) => set('endDate', e.target.value)} />
                <input type="time" aria-label="Hora de fin" value={form.endTime} onChange={(e) => set('endTime', e.target.value)} />
              </div>
            </Field>
            <div className="field field-end">
              <button type="submit" className="btn btn-primary">
                {saving ? <Spinner size={16} /> : <Plus size={16} aria-hidden />} Guardar bloqueo
              </button>
            </div>
          </div>
        </fieldset>
      </form>

      <h3 className="section-title">Bloqueos registrados</h3>
      {error && <ErrorBanner message={error} onRetry={reload} />}
      {loading && !data ? (
        <div className="center-pad"><Spinner /></div>
      ) : (data ?? []).length === 0 ? (
        <EmptyState icon={CalendarX} message="No hay bloqueos registrados entre el mes pasado y los próximos 6 meses" />
      ) : (
        <div className="table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Médico</th>
                <th>Motivo</th>
                <th>Tipo</th>
                <th>Desde</th>
                <th>Hasta</th>
                <th className="col-actions">Acción</th>
              </tr>
            </thead>
            <tbody>
              {(data ?? []).map((b) => (
                <tr key={b.id}>
                  <td>{b.doctorName}</td>
                  <td className="strong">
                    {b.title}
                    {b.reason && <div className="muted small">{b.reason}</div>}
                  </td>
                  <td><Badge tone="info">{BLOCK_TYPE_LABEL[b.blockType] ?? b.blockType}</Badge></td>
                  <td className="nowrap">{formatDateTime(b.startDateTime)}</td>
                  <td className="nowrap">{formatDateTime(b.endDateTime)}</td>
                  <td className="col-actions">
                    <button className="icon-button text-danger" title="Eliminar bloqueo" aria-label="Eliminar bloqueo" onClick={() => onDelete(b)}>
                      <Trash2 size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

function SystemTab() {
  const { user } = useAuth();
  const { data, loading, error, reload } = useApi(() => systemApi.info(), []);
  return (
    <>
      <h3 className="section-title">Servidor</h3>
      {error && <ErrorBanner message={error} onRetry={reload} />}
      {loading && !data ? (
        <div className="center-pad"><Spinner /></div>
      ) : (
        <dl className="info-list">
          <dt>Backend</dt>
          <dd>{data ? `${data.name} ${data.version}` : '-'}</dd>
          <dt>Versión de la API</dt>
          <dd className="mono">{data?.apiVersion ?? '-'} ({apiBaseUrl})</dd>
          <dt>Google Calendar</dt>
          <dd>
            <Badge tone={data?.googleCalendarEnabled ? 'success' : 'neutral'}>
              {data?.googleCalendarEnabled ? `Habilitado (${data.googleCalendarId ?? 'primary'})` : 'Deshabilitado'}
            </Badge>
          </dd>
        </dl>
      )}
      <h3 className="section-title">Sesión</h3>
      <dl className="info-list">
        <dt>Usuario</dt>
        <dd>{user ? `${user.fullName} (${user.username})` : '-'}</dd>
        <dt>Rol</dt>
        <dd>{user?.roleDisplayName ?? '-'}</dd>
        <dt>Interfaz</dt>
        <dd>React 18 + Vite</dd>
        <dt>Modo de datos</dt>
        <dd>{import.meta.env.VITE_USE_MOCK === 'true' ? <Badge tone="warning">API simulada (datos de ejemplo)</Badge> : <Badge tone="success">API real</Badge>}</dd>
      </dl>
    </>
  );
}
