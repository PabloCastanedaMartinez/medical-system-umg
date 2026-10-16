// Utilidades de fecha. El backend usa LocalDate/LocalDateTime sin zona horaria,
// por eso todo se interpreta y se envía en la hora local del navegador (sin "Z").

const pad = (n: number) => String(n).padStart(2, '0');

/** "2026-10-15T09:30:00" -> Date (hora local). */
export function parseLocalDateTime(value: string | null | undefined): Date | null {
  if (!value) return null;
  // Instantes con zona (2026-10-15T15:30:00Z o -06:00): los convierte el navegador.
  if (/T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})$/.test(value)) {
    const d = new Date(value);
    return Number.isNaN(d.getTime()) ? null : d;
  }
  const m = /^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2}))?)?/.exec(value);
  if (!m) return null;
  return new Date(+m[1], +m[2] - 1, +m[3], m[4] ? +m[4] : 0, m[5] ? +m[5] : 0, m[6] ? +m[6] : 0);
}

/** Date -> "2026-10-15" */
export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

/** Date -> "2026-10-15T09:30:00" (LocalDateTime) */
export function toIsoDateTime(date: Date): string {
  return `${toIsoDate(date)}T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

/** "2026-10-15" + "09:30" -> "2026-10-15T09:30:00" */
export function combineDateTime(isoDate: string, time: string): string {
  const [h = '00', m = '00'] = time.split(':');
  return `${isoDate}T${h.padStart(2, '0')}:${m.padStart(2, '0')}:00`;
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
