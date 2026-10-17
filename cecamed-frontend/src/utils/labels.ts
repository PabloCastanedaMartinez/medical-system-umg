import type {
  AppointmentStatus,
  BlockType,
  BloodType,
  DayOfWeek,
  DocumentType,
  Gender,
  GoogleSyncStatus,
} from '../api/types';

export type Tone = 'success' | 'warning' | 'danger' | 'info' | 'neutral';

export const APPOINTMENT_STATUS: Record<AppointmentStatus, { label: string; tone: Tone }> = {
  PROGRAMADA: { label: 'Programada', tone: 'info' },
  CONFIRMADA: { label: 'Confirmada', tone: 'success' },
  EN_SALA: { label: 'En sala', tone: 'warning' },
  ATENDIDA: { label: 'Atendida', tone: 'success' },
  REPROGRAMADA: { label: 'Reprogramada', tone: 'info' },
  NO_ASISTIO: { label: 'No asistió', tone: 'danger' },
  CANCELADA: { label: 'Cancelada', tone: 'danger' },
};

export const APPOINTMENT_STATUS_ORDER: AppointmentStatus[] = [
  'PROGRAMADA',
  'CONFIRMADA',
  'EN_SALA',
  'ATENDIDA',
  'CANCELADA',
  'REPROGRAMADA',
  'NO_ASISTIO',
];

export const GOOGLE_SYNC: Record<GoogleSyncStatus, { label: string; tone: Tone }> = {
  SYNCED: { label: 'Sincronizado', tone: 'success' },
  PENDING: { label: 'Pendiente', tone: 'warning' },
  FAILED: { label: 'Error al sincronizar', tone: 'danger' },
  NOT_APPLICABLE: { label: 'Sin calendario', tone: 'neutral' },
};

export const GENDER_LABEL: Record<Gender, string> = {
  MASCULINO: 'Masculino',
  FEMENINO: 'Femenino',
};

export const GENDER_SHORT: Record<Gender, string> = {
  MASCULINO: 'M',
  FEMENINO: 'F',
};

export const BLOOD_TYPE_SHORT: Record<BloodType, string> = {
  A_POSITIVO: 'A+',
  A_NEGATIVO: 'A-',
  B_POSITIVO: 'B+',
  B_NEGATIVO: 'B-',
  AB_POSITIVO: 'AB+',
  AB_NEGATIVO: 'AB-',
  O_POSITIVO: 'O+',
  O_NEGATIVO: 'O-',
  DESCONOCIDO: 'N/D',
};

export const BLOOD_TYPE_LONG: Record<BloodType, string> = {
  A_POSITIVO: 'A+ (A Positivo)',
  A_NEGATIVO: 'A- (A Negativo)',
  B_POSITIVO: 'B+ (B Positivo)',
  B_NEGATIVO: 'B- (B Negativo)',
  AB_POSITIVO: 'AB+ (AB Positivo)',
  AB_NEGATIVO: 'AB- (AB Negativo)',
  O_POSITIVO: 'O+ (O Positivo)',
  O_NEGATIVO: 'O- (O Negativo)',
  DESCONOCIDO: 'Desconocido / Sin tipificar',
};

export const BLOCK_TYPE_LABEL: Record<BlockType, string> = {
  VACACIONES: 'Vacaciones',
  FERIADO: 'Feriado',
  CAPACITACION: 'Capacitación',
  PERSONAL: 'Personal',
  MANTENIMIENTO: 'Mantenimiento',
  OTRO: 'Otro',
};

export const DOCUMENT_TYPE_LABEL: Record<DocumentType, string> = {
  ESTUDIO_LABORATORIO: 'Estudio de laboratorio',
  IMAGEN_RAYOS_X: 'Imagen / Rayos X',
  RECETA_MEDICA: 'Receta médica',
  INFORME_EXTERNO: 'Informe externo',
  CONSENTIMIENTO_INFORMADO: 'Consentimiento informado',
  OTRO: 'Otro',
};

export const DAY_LABEL: Record<DayOfWeek, string> = {
  MONDAY: 'Lunes',
  TUESDAY: 'Martes',
  WEDNESDAY: 'Miércoles',
  THURSDAY: 'Jueves',
  FRIDAY: 'Viernes',
  SATURDAY: 'Sábado',
  SUNDAY: 'Domingo',
};

export const DAY_ORDER: DayOfWeek[] = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'];

/** Estados finales en los que ya no se puede reprogramar, completar ni cancelar (agenda). */
export function isFinalized(status: AppointmentStatus): boolean {
  return status === 'ATENDIDA' || status === 'CANCELADA';
}

/** Estados en los que recepción ya no tiene acciones disponibles. */
export function isClosedForReception(status: AppointmentStatus): boolean {
  return status === 'ATENDIDA' || status === 'CANCELADA' || status === 'NO_ASISTIO';
}
