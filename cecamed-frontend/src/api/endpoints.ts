import { API_BASE_URL, ApiError, buildUrl, getAccessToken, request, setAccessToken } from './client';
import type {
  Appointment,
  AppointmentRequest,
  AppointmentReschedule,
  AppointmentStatus,
  AvailableSlot,
  Consultation,
  ConsultationRequest,
  DashboardSummary,
  DoctorSchedule,
  DocumentType,
  LoginResponse,
  MedicalRecord,
  Patient,
  PatientDocument,
  PatientRequest,
  PatientStatusFilter,
  ScheduleBlock,
  ScheduleBlockRequest,
  SessionUser,
  SystemInfo,
} from './types';

export const authApi = {
  async login(username: string, password: string): Promise<SessionUser> {
    const res = await request<LoginResponse>('/auth/login', { method: 'POST', body: { username, password }, silentAuth: true });
    setAccessToken(res.accessToken ?? res.token ?? null);
    return res;
  },
  async logout(): Promise<void> {
    try {
      await request<void>('/auth/logout', { method: 'POST', silentAuth: true });
    } catch {
      // Con tokens el cierre de sesión es local; el backend puede no tener este endpoint.
    } finally {
      setAccessToken(null);
    }
  },
  me: () => request<SessionUser>('/auth/me', { silentAuth: true }),
};

export const dashboardApi = {
  summary: () => request<DashboardSummary>('/dashboard/summary'),
};

export const patientsApi = {
  list: (search: string, status: PatientStatusFilter, signal?: AbortSignal) =>
    request<Patient[]>('/patients', { query: { search, status }, signal }),
  get: (id: number) => request<Patient>(`/patients/${id}`),
  create: (body: PatientRequest) => request<Patient>('/patients', { method: 'POST', body }),
  update: (id: number, body: PatientRequest) => request<Patient>(`/patients/${id}`, { method: 'PUT', body }),
  activate: (id: number) => request<void>(`/patients/${id}/activate`, { method: 'PATCH' }),
  deactivate: (id: number) => request<void>(`/patients/${id}/deactivate`, { method: 'PATCH' }),
  appointments: (id: number) => request<Appointment[]>(`/patients/${id}/appointments`),
};

export const medicalRecordApi = {
  get: (patientId: number) => request<MedicalRecord>(`/patients/${patientId}/medical-record`),
  update: (patientId: number, body: MedicalRecord) =>
    request<MedicalRecord>(`/patients/${patientId}/medical-record`, { method: 'PUT', body }),
  consultations: (patientId: number) => request<Consultation[]>(`/patients/${patientId}/consultations`),
  createConsultation: (body: ConsultationRequest) =>
    request<Consultation>('/consultations', { method: 'POST', body }),
};

export const documentsApi = {
  list: (patientId: number) => request<PatientDocument[]>(`/patients/${patientId}/documents`),
  upload: (patientId: number, file: File, documentType: DocumentType, description: string) => {
    const form = new FormData();
    form.append('file', file);
    form.append('documentType', documentType);
    if (description.trim()) form.append('description', description.trim());
    return request<PatientDocument>(`/patients/${patientId}/documents`, { method: 'POST', body: form });
  },
  remove: (id: number) => request<void>(`/documents/${id}`, { method: 'DELETE' }),
  /** Descarga el archivo con el token (un enlace <a> no puede enviar Authorization). */
  async fetchFile(id: number): Promise<Blob> {
    const token = getAccessToken();
    const res = await fetch(buildUrl(`/documents/${id}/file`), { headers: token ? { Authorization: `Bearer ${token}` } : {} });
    if (!res.ok) throw new ApiError(res.status, 'No fue posible obtener el documento', null);
    return res.blob();
  },
};

export const appointmentsApi = {
  list: (start: string, end: string, status?: AppointmentStatus | null, signal?: AbortSignal) =>
    request<Appointment[]>('/appointments', { query: { start, end, status }, signal }),
  availableSlots: (date: string) => request<AvailableSlot[]>('/appointments/available-slots', { query: { date } }),
  create: (body: AppointmentRequest) => request<Appointment>('/appointments', { method: 'POST', body }),
  reschedule: (id: number, body: AppointmentReschedule) =>
    request<Appointment>(`/appointments/${id}/reschedule`, { method: 'PUT', body }),
  cancel: (id: number, reason: string) =>
    request<Appointment>(`/appointments/${id}/cancel`, { method: 'POST', body: { reason } }),
  complete: (id: number) => request<Appointment>(`/appointments/${id}/complete`, { method: 'POST' }),
  updateStatus: (id: number, status: AppointmentStatus) =>
    request<Appointment>(`/appointments/${id}/status`, { method: 'PATCH', body: { status } }),
};

export const scheduleApi = {
  schedules: () => request<DoctorSchedule[]>('/schedules'),
  blocks: () => request<ScheduleBlock[]>('/schedule-blocks'),
  createBlock: (body: ScheduleBlockRequest) =>
    request<ScheduleBlock>('/schedule-blocks', { method: 'POST', body }),
  deleteBlock: (id: number) => request<void>(`/schedule-blocks/${id}`, { method: 'DELETE' }),
};

// Base actual de la API (útil para mostrarla en Configuración).
export const apiBaseUrl = API_BASE_URL;

export const systemApi = {
  info: () => request<SystemInfo>('/system/info'),
};
