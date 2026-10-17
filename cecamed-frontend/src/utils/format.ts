// Utilidades de fecha y hora.
//
// Contrato con cecamed-api (docs/api-contract.md y docs/architecture.md):
//   - fechas de calendario en "YYYY-MM-DD";
//   - instantes en ISO 8601 con offset ("2026-10-15T09:30:00-06:00");
//   - la agenda y la presentación usan la zona de la clínica, America/Guatemala.
//
// En la interfaz, toda fecha-hora se maneja como "hora de la clínica": un Date cuyos campos
// (getHours, getDate...) son la hora en America/Guatemala, sin importar la zona del navegador.
// Así un equipo configurado en UTC o en otro país muestra 09:30 igual que uno en Guatemala.
// La conversión entre instante y hora de la clínica ocurre solo en este archivo:
//   - al leer:   parseLocalDateTime(instante del backend) -> hora de la clínica;
//   - al enviar: toIsoDateTime / combineDateTime -> instante con el offset de la clínica;
//   - "ahora":   clinicNow() en lugar de new Date().

const pad = (n: number) => String(n).padStart(2, '0');

/** Zona horaria de la clínica (la del CECAMed original). */
export const CLINIC_TIME_ZONE = 'America/Guatemala';

const clinicParts = new Intl.DateTimeFormat('en-US', {
  timeZone: CLINIC_TIME_ZONE,
  hourCycle: 'h23',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  second: '2-digit',
});

/** Campos [año, mes, día, hora, minuto, segundo] de un instante en la zona de la clínica. */
function clinicFields(epochMs: number): number[] {
  const values: Record<string, number> = {};
  for (const part of clinicParts.formatToParts(new Date(epochMs))) {
    if (part.type !== 'literal') values[part.type] = Number(part.value);
  }
  return [values.year, values.month, values.day, values.hour === 24 ? 0 : values.hour, values.minute, values.second];
}

/** Diferencia en minutos entre la hora de la clínica y UTC en ese instante (Guatemala: -360). */
function clinicOffsetMinutes(epochMs: number): number {
  const [y, mo, d, h, mi, s] = clinicFields(epochMs);
  const wallAsUtc = Date.UTC(y, mo - 1, d, h, mi, s);
  return Math.round((wallAsUtc - Math.floor(epochMs / 1000) * 1000) / 60_000);
}

/** Instante real -> Date con la hora de la clínica en sus campos. */
function instantToClinic(instant: Date): Date {
  const [y, mo, d, h, mi, s] = clinicFields(instant.getTime());
  return new Date(y, mo - 1, d, h, mi, s, instant.getMilliseconds());
}

/** Date con hora de la clínica -> milisegundos del instante real. */
function clinicToEpochMs(wall: Date): number {
  const wallAsUtc = Date.UTC(
    wall.getFullYear(),
    wall.getMonth(),
    wall.getDate(),
    wall.getHours(),
    wall.getMinutes(),
    wall.getSeconds(),
    wall.getMilliseconds(),
  );
  let epoch = wallAsUtc - clinicOffsetMinutes(wallAsUtc) * 60_000;
  // Segunda pasada por si el offset cambia cerca de la hora calculada (horario de verano).
  epoch = wallAsUtc - clinicOffsetMinutes(epoch) * 60_000;
  return epoch;
}

function formatOffset(minutes: number): string {
  const sign = minutes < 0 ? '-' : '+';
  const abs = Math.abs(minutes);
  return `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`;
}

/** Fecha y hora actuales en la clínica. Usar en lugar de `new Date()`. */
export function clinicNow(): Date {
  return instantToClinic(new Date());
}

/**
 * Texto del backend -> Date en hora de la clínica.
 * - Instante con zona ("…Z" o "…-06:00"): se convierte a la hora de la clínica.
 * - Sin zona ("2026-10-15T09:30:00" o "2026-10-15"): ya es hora de la clínica.
 */
export function parseLocalDateTime(value: string | null | undefined): Date | null {
  if (!value) return null;
  if (/T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})$/.test(value)) {
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? null : instantToClinic(d);
  }
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(value);
  if (!m) return null;
  return new Date(+m[1], +m[2] - 1, +m[3], m[4] ? +m[4] : 0, m[5] ? +m[5] : 0, m[6] ? +m[6] : 0);
}

/** Date -> "2026-10-15" */
export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Hora de la clínica -> instante ISO 8601 con offset: "2026-10-15T09:30:00-06:00". */
export function toIsoDateTime(date: Date): string {
  const wallAsUtc = Date.UTC(date.getFullYear(), date.getMonth(), date.getDate(), date.getHours(), date.getMinutes(), date.getSeconds());
  const offset = Math.round((wallAsUtc - Math.floor(clinicToEpochMs(date) / 1000) * 1000) / 60_000);
  return `${toIsoDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}${formatOffset(offset)}`;
}

/** "2026-10-15" + "09:30" (hora de la clínica) -> "2026-10-15T09:30:00-06:00" */
export function combineDateTime(isoDate: string, time: string): string {
  const [h = '00', m = '00'] = time.split(':');
  const wall = parseLocalDateTime(`${isoDate}T${h.padStart(2, '0')}:${m.padStart(2, '0')}:00`);
  if (!wall) throw new Error(`Fecha u hora inválida: ${isoDate} ${time}`);
  return toIsoDateTime(wall);
}

export function startOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
}

export function endOfDay(date: Date): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 23, 59, 59, 999);
}

export function addDays(date: Date, days: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + days);
  return d;
}

export function addMonths(date: Date, months: number): Date {
  const d = new Date(date.getFullYear(), date.getMonth() + months, 1);
  const lastDay = new Date(d.getFullYear(), d.getMonth() + 1, 0).getDate();
  d.setDate(Math.min(date.getDate(), lastDay));
  return d;
}

/** Lunes de la semana de `date`. */
export function startOfWeek(date: Date): Date {
  const d = startOfDay(date);
  const diff = (d.getDay() + 6) % 7; // 0 = lunes
  return addDays(d, -diff);
}

const LOCALE = 'es-GT';

export function formatDate(value: string | Date | null | undefined): string {
  const d = typeof value === 'string' ? parseLocalDateTime(value) : value ?? null;
  if (!d) return '-';
  return `${pad(d.getDate())}/${pad(d.getMonth() + 1)}/${d.getFullYear()}`;
}

export function formatTime(value: string | Date | null | undefined): string {
  const d = typeof value === 'string' ? parseLocalDateTime(value) : value ?? null;
  if (!d) return '--:--';
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export function formatDateTime(value: string | null | undefined): string {
  const d = parseLocalDateTime(value);
  if (!d) return '-';
  return `${formatDate(d)} ${formatTime(d)}`;
}

/** "08:00:00" -> "08:00" */
export function formatLocalTime(value: string | null | undefined): string {
  if (!value) return '--:--';
  return value.slice(0, 5);
}

export function capitalize(text: string): string {
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
}

/** "Jueves, 15 de octubre de 2026" */
export function formatLongDate(date: Date): string {
  return capitalize(
    date.toLocaleDateString(LOCALE, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }),
  );
}

export function formatMonthYear(date: Date): string {
  return capitalize(date.toLocaleDateString(LOCALE, { month: 'long', year: 'numeric' }));
}

export function formatDayMonth(date: Date): string {
  return date.toLocaleDateString(LOCALE, { day: 'numeric', month: 'long' });
}

export function formatClock(date: Date): string {
  const day = date.toLocaleDateString(LOCALE, { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' });
  return `${capitalize(day)} • ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** Diferencia "12 min 05 s" entre dos fechas. */
export function formatElapsed(from: Date, to: Date): string {
  const totalSeconds = Math.max(0, Math.floor((to.getTime() - from.getTime()) / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  if (hours > 0) return `${hours} h ${pad(minutes)} min`;
  return `${minutes} min ${pad(seconds)} s`;
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

/** Texto vacío -> null; en otro caso, recortado. */
export function blankToNull(value: string | null | undefined): string | null {
  const trimmed = (value ?? '').trim();
  return trimmed === '' ? null : trimmed;
}

export function parseIntOrNull(value: string): number | null {
  const trimmed = value.trim();
  if (!trimmed) return null;
  const n = Number.parseInt(trimmed, 10);
  return Number.isFinite(n) ? n : null;
}

export function parseDecimalOrNull(value: string): number | null {
  const trimmed = value.trim().replace(',', '.');
  if (!trimmed) return null;
  const n = Number.parseFloat(trimmed);
  return Number.isFinite(n) ? n : null;
}
