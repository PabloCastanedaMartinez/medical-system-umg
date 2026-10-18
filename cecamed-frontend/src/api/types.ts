// Tipos que reflejan los DTO de cecamed-services y cecamed-web.
// Fechas: LocalDateTime -> "2026-10-15T09:30:00", LocalDate -> "2026-10-15", LocalTime -> "08:00:00".

export type UserRole = 'MEDICO' | 'RECEPCION' | 'ADMIN' | 'PACIENTE';

export interface SessionUser {
  username: string;
  fullName: string;
  role: UserRole;
  roleDisplayName: string;
  loginTime: string;
}

export type Gender = 'MASCULINO' | 'FEMENINO';

export type BloodType =
  | 'A_POSITIVO'
  | 'A_NEGATIVO'
  | 'B_POSITIVO'
  | 'B_NEGATIVO'
  | 'AB_POSITIVO'
  | 'AB_NEGATIVO'
  | 'O_POSITIVO'
  | 'O_NEGATIVO'
  | 'DESCONOCIDO';

export interface Patient {
  version: number;
  id: number;
  firstName: string;
  lastName: string;
  fullName: string;
  identificationNumber?: string | null;
  birthDate: string | null;
  age: number | null;
  gender: Gender | null;
  bloodType: BloodType | null;
  phone: string | null;
  email: string | null;
  address: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelationship: string | null;
  notes: string | null;
  active: boolean | null;
  medicalRecordNumber: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface PatientRequest {
  allergies?: string;
  version?: number;
  identificationNumber?: string | null;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: Gender;
  bloodType: BloodType;
  phone: string | null;
  email: string | null;
  address: string | null;
  emergencyContactName: string | null;
  emergencyContactPhone: string | null;
  emergencyContactRelationship: string | null;
  notes: string | null;
}

export type PatientStatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

export type AppointmentStatus =
  | 'PENDIENTE'
  | 'PROGRAMADA'
  | 'CONFIRMADA'
  | 'EN_SALA'
  | 'ATENDIDA'
  | 'CANCELADA'
  | 'REPROGRAMADA'
  | 'NO_ASISTIO';

export type GoogleSyncStatus = 'PENDING' | 'SYNCED' | 'FAILED' | 'NOT_APPLICABLE';

export interface Appointment {
  doctorId: number;
  doctorName: string;
  specialty: string;
  version: number;
  id: number;
  patientId: number;
  patientFullName: string | null;
  patientIdentificationNumber?: string | null;
  patientPhone: string | null;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  reasonForVisit: string | null;
  cancellationReason: string | null;
  notes: string | null;
  googleEventId: string | null;
  googleSyncStatus: GoogleSyncStatus | null;
  googleHtmlLink: string | null;
  googleLastSyncedAt: string | null;
  /** Hora de llegada a sala (check-in). La fija el backend al pasar la cita a EN_SALA. */
  checkedInAt?: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface AppointmentRequest {
  doctorId: number;
  patientId: number;
  startTime: string;
  endTime: string;
  reasonForVisit: string;
  notes: string | null;
}

export interface AppointmentReschedule {
  version: number;
  newStartTime: string;
  newEndTime: string;
  reason: string;
}

export interface AvailableSlot {
  doctorId: number;
  startTime: string;
  endTime: string;
  available: boolean;
  reasonIfNotAvailable: string | null;
}

export interface MedicalRecord {
  version: number;
  patientId: number;
  recordNumber: string;
  allergies: string | null;
  pathologicalHistory: string | null;
  nonPathologicalHistory: string | null;
  familyHistory: string | null;
  surgicalHistory: string | null;
  traumaticHistory: string | null;
  currentMedications: string | null;
  generalObservations: string | null;
  gynecologicalObstetricHistory: string | null;
  waterGlassesPerDay: number | null;
  mealsPerDay: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface VitalSigns {
  systolicPressure: number | null;
  diastolicPressure: number | null;
  heartRate: number | null;
  respiratoryRate: number | null;
  temperatureCelsius: number | null;
  weightKg: number | null;
  heightCm: number | null;
  bmi?: number | null;
  oxygenSaturationPercentage: number | null;
}

export type DocumentType =
  | 'ESTUDIO_LABORATORIO'
  | 'IMAGEN_RAYOS_X'
  | 'RECETA_MEDICA'
  | 'INFORME_EXTERNO'
  | 'CONSENTIMIENTO_INFORMADO'
  | 'OTRO';

export interface PatientDocument {
  id: number;
  patientId: number;
  consultationId: number | null;
  fileName: string;
  originalFileName: string | null;
  fileType: string | null;
  documentType: DocumentType;
  fileSizeBytes: number | null;
  checksumSha256: string | null;
  description: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface Consultation {
  version: number;
  consultationType: string;
  doctorName: string;
  prescriptions: string | null;
  followUpInstructions: string | null;
  id: number;
  patientId: number;
  patientFullName: string | null;
  consultationDateTime: string;
  reason: string;
  symptoms: string | null;
  physicalExamination: string | null;
  vitalSigns: VitalSigns | null;
  diagnosis: string;
  icd10Code: string | null;
  treatmentPlan: string | null;
  privateNotes: string | null;
  attachedDocuments: PatientDocument[] | null;
  createdAt: string | null;
  updatedAt: string | null;
}

export interface ConsultationRequest {
  patientId: number;
  reason: string;
  symptoms: string | null;
  physicalExamination: string | null;
  vitalSigns: VitalSigns;
  diagnosis: string;
  icd10Code: string | null;
  treatmentPlan: string | null;
  privateNotes: string | null;
}

export type DayOfWeek = 'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY' | 'SATURDAY' | 'SUNDAY';

export interface DoctorSchedule {
  doctorId: number;
  doctorName: string;
  id: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number | null;
  isActive: boolean | null;
}

export type BlockType = 'VACACIONES' | 'FERIADO' | 'CAPACITACION' | 'PERSONAL' | 'MANTENIMIENTO' | 'OTRO';

export interface ScheduleBlock {
  doctorId: number;
  doctorName: string;
  id: number;
  title: string;
  startDateTime: string;
  endDateTime: string;
  blockType: BlockType;
  allDay: boolean | null;
  reason: string | null;
  googleEventId: string | null;
  createdAt: string | null;
}

export interface ScheduleBlockRequest {
  doctorId: number;
  title: string;
  startDateTime: string;
  endDateTime: string;
  blockType: BlockType;
  allDay: boolean;
  reason: string | null;
}

export interface DashboardSummary {
  appointmentsToday: number;
  waitingRoom: number;
  completedToday: number;
  pendingToday: number;
  activePatients: number;
  todayAppointments: Appointment[];
}

/** Respuesta de GET /api/v1/system/info. Los campos opcionales aún no los publica el backend. */
export interface SystemInfo {
  name: string;
  version: string;
  apiVersion: string;
  googleCalendarId?: string;
  googleCalendarEnabled?: boolean;
}

/** Respuesta esperada de POST /auth/login (pendiente de definir por el backend). */
export interface LoginResponse extends SessionUser {
  accessToken: string;
  refreshToken: string;
}

/** Problem Details (RFC 9457) con las propiedades `code` y `errors` del backend. */
export interface ApiErrorBody {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  code?: string;
  errors?: { field: string; message: string }[];
  /** Compatibilidad con respuestas que usen `message`. */
  message?: string;
}

export interface PagedResult<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Contrato HTTP de consultas; el adaptador conserva los nombres de presentación. */
export interface ConsultationResponse {
  consultationId: number;
  patientId: number;
  consultationDate: string;
  consultationType: string;
  doctorName: string;
  reasonForVisit: string;
  symptoms: string | null;
  examinationFindings: string | null;
  diagnosis: string;
  treatmentPlan: string | null;
  prescriptions: string | null;
  followUpInstructions: string | null;
  vitalSigns: string | null;
  notes: string | null;
  icd10Code: string | null;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface Doctor {
  id: number;
  fullName: string;
  specialty: string;
  username: string;
  active: boolean;
  version: number;
}
export interface HoursRequest {
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  slotDurationMinutes: number;
}
export interface DoctorRequest {
  fullName: string;
  specialty: string;
  username: string;
  password?: string;
  schedules: HoursRequest[];
  version?: number;
}
export interface UserAccount {
  id: number;
  username: string;
  fullName: string;
  role: UserRole;
  active: boolean;
}
