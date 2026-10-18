import { API_BASE_URL, request, requestBlob, setSession, endSession } from './client';
import type {
  PagedResult, ConsultationResponse, Doctor, DoctorRequest, UserAccount, UserRole,
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
    setSession(res);
    return res;
  },
  async logout(): Promise<void> {
    try {
      const refreshToken = await endSession();
      await request<void>('/auth/logout', { method: 'POST', body: { refreshToken }, silentAuth: true });
    } catch {
      // Con tokens el cierre de sesión es local; el backend puede no tener este endpoint.
    } finally {
      setSession(null);
    }
  },
  me: () => request<SessionUser>('/auth/me', { silentAuth: true }),
};

export const dashboardApi = {
  summary: () => request<DashboardSummary>('/dashboard/summary'),
};

export const patientsApi = {
  page: (search: string, status: PatientStatusFilter, page = 0, signal?: AbortSignal) =>
    request<PagedResult<Patient>>('/patients', { query: { search, status, page, size: 20 }, signal }),
  async list(search: string, status: PatientStatusFilter, signal?: AbortSignal): Promise<Patient[]> {
    // Selectores existentes: recorrer todas las páginas, sin truncar pacientes silenciosamente.
    const patients: Patient[] = [];
    let page = 0;
    let result: PagedResult<Patient>;
    do {
      result = await request<PagedResult<Patient>>('/patients', { query: { search, status, page, size: 100 }, signal });
      patients.push(...result.content);
      page++;
    } while (page < result.totalPages);
    return patients;
  },
  get: (id: number) => request<Patient>(`/patients/${id}`),
  create: (body: PatientRequest) => request<Patient>('/patients', { method: 'POST', body }),
  update: (id: number, body: PatientRequest) => request<Patient>(`/patients/${id}`, { method: 'PUT', body }),
  activate: (id: number, version: number) => request<void>(`/patients/${id}/activate`, { method: 'PATCH', query: { version } }),
  deactivate: (id: number, version: number) => request<void>(`/patients/${id}/deactivate`, { method: 'PATCH', query: { version } }),
  appointments: (id: number) => request<Appointment[]>(`/patients/${id}/appointments`),
};

export const medicalRecordApi = {
  get: (patientId: number) => request<MedicalRecord>(`/patients/${patientId}/medical-record`),
  update: (patientId: number, body: MedicalRecord) =>
    request<MedicalRecord>(`/patients/${patientId}/medical-record`, { method: 'PUT', body }),
  consultations: async (patientId: number) =>
    (await request<ConsultationResponse[]>(`/patients/${patientId}/consultations`)).map(toConsultation),
  async updateConsultation(original: Consultation, body: ConsultationRequest): Promise<Consultation> {
    return toConsultation(await request<ConsultationResponse>(`/patients/${original.patientId}/consultations/${original.id}`, {
      method: 'PUT', body: {
        consultationType: original.consultationType, doctorName: original.doctorName,
        prescriptions: original.prescriptions, followUpInstructions: original.followUpInstructions,
        reasonForVisit: body.reason, symptoms: body.symptoms, examinationFindings: body.physicalExamination,
        diagnosis: body.diagnosis, icd10Code: body.icd10Code, treatmentPlan: body.treatmentPlan,
        vitalSigns: JSON.stringify(body.vitalSigns), notes: body.privateNotes, version: original.version,
      },
    }));
  },
  async createConsultation(body: ConsultationRequest): Promise<Consultation> {
    const user = await authApi.me();
    const result = await request<ConsultationResponse>(`/patients/${body.patientId}/consultations`, {
      method: 'POST', body: {
        consultationType: 'GENERAL', doctorName: user.fullName, reasonForVisit: body.reason,
        symptoms: body.symptoms, examinationFindings: body.physicalExamination,
        diagnosis: body.diagnosis, icd10Code: body.icd10Code, treatmentPlan: body.treatmentPlan,
        vitalSigns: JSON.stringify(body.vitalSigns), notes: body.privateNotes,
      },
    });
    return toConsultation(result);
  },
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
  /**
   * Descarga el archivo con el cliente común (un enlace <a> no puede enviar Authorization).
   * Así un 401 cierra la sesión y los errores muestran el mensaje del backend.
   */
  fetchFile: (id: number) => requestBlob(`/documents/${id}/file`),
};

export const appointmentsApi = {
  list: (start: string, end: string, status?: AppointmentStatus | null, signal?: AbortSignal, doctorId?: number) =>
    request<Appointment[]>('/appointments', { query: { start, end, status, doctorId }, signal }),
  availableSlots: (date: string, doctorId: number, excludingId?: number, signal?: AbortSignal) =>
    request<AvailableSlot[]>('/appointments/available-slots', { query: { date, doctorId, excludingId }, signal }),
  create: (body: AppointmentRequest) => request<Appointment>('/appointments', { method: 'POST', body }),
  reschedule: (id: number, body: AppointmentReschedule) =>
    request<Appointment>(`/appointments/${id}/reschedule`, { method: 'PUT', body }),
  cancel: (id: number, reason: string, version: number) =>
    request<Appointment>(`/appointments/${id}/cancel`, { method: 'POST', body: { reason, version } }),
  complete: (id: number, version: number) => request<Appointment>(`/appointments/${id}/status`, { method: 'PATCH', body: { status: 'ATENDIDA', version } }),
  updateStatus: (id: number, status: AppointmentStatus, version: number) =>
    request<Appointment>(`/appointments/${id}/status`, { method: 'PATCH', body: { status, version } }),
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

function toConsultation(c: ConsultationResponse): Consultation {
  let vitalSigns: Consultation['vitalSigns'] = null;
  try { vitalSigns = c.vitalSigns ? JSON.parse(c.vitalSigns) : null; } catch { /* Texto clínico legado */ }
  return {
    consultationType: c.consultationType, doctorName: c.doctorName,
    prescriptions: c.prescriptions, followUpInstructions: c.followUpInstructions,
    id: c.consultationId, patientId: c.patientId, patientFullName: null,
    consultationDateTime: c.consultationDate, reason: c.reasonForVisit,
    symptoms: c.symptoms, physicalExamination: c.examinationFindings, vitalSigns,
    diagnosis: c.diagnosis, icd10Code: c.icd10Code, treatmentPlan: c.treatmentPlan,
    privateNotes: c.notes, attachedDocuments: null, createdAt: c.createdAt,
    updatedAt: c.updatedAt, version: c.version,
  };
}

export const doctorsApi = {
  list: () => request<Doctor[]>('/doctors'),
  create: (body: DoctorRequest) => request<Doctor>('/doctors', { method: 'POST', body }),
  update: (id: number, body: DoctorRequest) => request<Doctor>(`/doctors/${id}`, { method: 'PUT', body }),
  today: () => request<Appointment[]>('/doctors/me/agenda'),
};
export const portalApi = {
  profile: () => request<Patient>('/portal/me'),
  history: () => request<Appointment[]>('/portal/appointments'),
  create: (body: AppointmentRequest) => request<Appointment>('/portal/appointments', { method: 'POST', body: {
    doctorId: body.doctorId, startTime: body.startTime, endTime: body.endTime, reasonForVisit: body.reasonForVisit,
  } }),
  grantAccess: (patientId: number, username: string, password: string) =>
    request<void>(`/patients/${patientId}/portal-access`, { method: 'POST', body: { username, password } }),
};
export const usersApi = {
  list: () => request<PagedResult<UserAccount>>('/users', { query: { size: 100 } }),
  create: (body: { username: string; password: string; fullName: string; role: UserRole }) =>
    request<UserAccount>('/users', { method: 'POST', body }),
  setActive: (user: UserAccount) => request<UserAccount>(`/users/${user.id}/${user.active ? 'deactivate' : 'activate'}`, { method: 'PATCH' }),
};
