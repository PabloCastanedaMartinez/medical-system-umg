// @ts-nocheck
/*
 * API simulada en el navegador (modo `npm run dev:mock`).
 * Permite usar y probar el frontend mientras el backend del equipo (cecamed-api) aún no publica
 * los endpoints de negocio. Los datos son de ejemplo y se reinician al recargar la página.
 * No se incluye en la compilación normal (`npm run build`).
 */
import { clinicNow, parseLocalDateTime, toIsoDateTime } from '../utils/format';

// Igual que el backend: instantes con offset de la clínica ("2026-10-15T09:30:00-06:00").
const iso = (d) => toIsoDateTime(d);
const parse = (s) => parseLocalDateTime(s);
const now = () => clinicNow();

const USERS = {
  medico: { password: 'medico123', fullName: 'Dr. Angel Anibal Mazariegos Pinales', role: 'MEDICO', roleDisplayName: 'Médico Especialista' },
  recepcion: { password: 'recepcion123', fullName: 'Licda. Sofía Méndez', role: 'RECEPCION', roleDisplayName: 'Recepción Clínica' },
};
// Token simulado: "mock-<usuario>". Sobrevive a recargas porque el cliente lo guarda en sessionStorage.
const sessionFromToken = (token) => {
  const username = token.replace(/^mock-/, '');
  const u = USERS[username];
  return u ? { username, fullName: u.fullName, role: u.role, roleDisplayName: u.roleDisplayName, loginTime: iso(now()) } : null;
};
let seq = 100;

const patients = [
  ['María José', 'López Herrera', '1985-03-12', 'FEMENINO', 'O_POSITIVO', '5555-1234'],
  ['Carlos Eduardo', 'Ramírez Pérez', '1972-11-02', 'MASCULINO', 'A_POSITIVO', '5555-9876'],
  ['Ana Lucía', 'Morales', '1999-07-21', 'FEMENINO', 'B_NEGATIVO', '4444-1122'],
  ['José Antonio', 'García Orellana', '1960-01-30', 'MASCULINO', 'DESCONOCIDO', null],
  ['Lucía Fernanda', 'Castillo Ruiz', '2010-05-15', 'FEMENINO', 'AB_POSITIVO', '3333-7788'],
].map(([firstName, lastName, birthDate, gender, bloodType, phone], i) => ({
  id: i + 1, firstName, lastName, fullName: `${firstName} ${lastName}`, birthDate,
  age: now().getFullYear() - Number(birthDate.slice(0, 4)), gender, bloodType, phone,
  email: i === 0 ? 'maria.lopez@correo.com' : null, address: null, emergencyContactName: null,
  emergencyContactPhone: null, emergencyContactRelationship: null, notes: null, active: i !== 3,
  medicalRecordNumber: `EXP-2026-${String(10234 + i * 1111).padStart(5, '0')}`, createdAt: iso(now()), updatedAt: iso(now()),
}));
const records = Object.fromEntries(patients.map((p) => [p.id, {
  id: p.id, patientId: p.id, recordNumber: p.medicalRecordNumber,
  allergies: p.id === 1 ? 'Penicilina' : null, pathologicalHistory: p.id === 2 ? 'Hipertensión arterial desde 2015' : null,
  nonPathologicalHistory: null, familyHistory: null, surgicalHistory: null, traumaticHistory: null, currentMedications: p.id === 2 ? 'Losartán 50 mg c/24h' : null,
  generalObservations: null, gynecologicalObstetricHistory: null, waterGlassesPerDay: 6, mealsPerDay: 3,
}]));
const consultations = [{
  id: 1, patientId: 2, patientFullName: patients[1].fullName, consultationDateTime: iso(new Date(now().getTime() - 20 * 864e5)),
  reason: 'Control de presión arterial', symptoms: 'Cefalea ocasional', physicalExamination: 'Paciente consciente, orientado.',
  vitalSigns: { systolicPressure: 138, diastolicPressure: 88, heartRate: 78, respiratoryRate: 16, temperatureCelsius: 36.6, weightKg: 82.5, heightCm: 172, bmi: 27.89, oxygenSaturationPercentage: 97 },
  diagnosis: 'Hipertensión esencial (primaria)', icd10Code: 'I10', treatmentPlan: 'Continuar Losartán 50 mg. Dieta baja en sodio.', privateNotes: null, attachedDocuments: [],
}];
const documents = [{ id: 1, patientId: 2, consultationId: null, fileName: 'a1.pdf', originalFileName: 'Hemograma_marzo.pdf', fileType: 'application/pdf', documentType: 'ESTUDIO_LABORATORIO', fileSizeBytes: 245000, description: 'Hemograma completo', createdAt: iso(now()) }];

const today = clinicNow(); today.setHours(0, 0, 0, 0);
const at = (h, m, dayOffset = 0) => { const d = new Date(today); d.setDate(d.getDate() + dayOffset); d.setHours(h, m, 0, 0); return d; };
const mkAppt = (id, pid, start, status, reason) => {
  const p = patients.find((x) => x.id === pid);
  return { id, patientId: pid, patientFullName: p.fullName, patientPhone: p.phone, startTime: iso(start), endTime: iso(new Date(start.getTime() + 30 * 6e4)),
    status, reasonForVisit: reason, cancellationReason: null, notes: null, googleEventId: null, googleSyncStatus: 'NOT_APPLICABLE',
    googleHtmlLink: null, googleLastSyncedAt: null, checkedInAt: status === 'EN_SALA' ? iso(new Date(now().getTime() - 17 * 6e4)) : null,
    createdAt: iso(now()), updatedAt: iso(now()) };
};
const appointments = [
  mkAppt(1, 1, at(8, 0), 'ATENDIDA', 'Chequeo general'),
  mkAppt(2, 2, at(9, 0), 'EN_SALA', 'Control de presión arterial'),
  mkAppt(3, 3, at(10, 30), 'CONFIRMADA', 'Dolor abdominal'),
  mkAppt(4, 5, at(11, 0), 'PROGRAMADA', 'Vacunación'),
  mkAppt(5, 1, at(15, 0), 'PROGRAMADA', 'Resultados de laboratorio'),
  mkAppt(6, 2, at(9, 30, 2), 'PROGRAMADA', 'Seguimiento'),
];
const schedules = ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'].map((d, i) => ({ id: i + 1, dayOfWeek: d, startTime: '08:00:00', endTime: d === 'SATURDAY' ? '12:00:00' : '17:00:00', slotDurationMinutes: 30, isActive: true }));
const blocks = [{ id: 1, title: 'Almuerzo', startDateTime: iso(at(12, 0)), endDateTime: iso(at(13, 0)), blockType: 'PERSONAL', allDay: false, reason: null, googleEventId: null, createdAt: iso(now()) }];

const send = (res, status, body) => {
  res.r = status === 204 || body === undefined ? new Response(null, { status }) : new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
};
const CODES = { 400: 'VALIDATION_ERROR', 401: 'AUTHENTICATION_REQUIRED', 403: 'ACCESS_DENIED', 404: 'RESOURCE_NOT_FOUND', 409: 'BUSINESS_RULE', 422: 'BUSINESS_RULE', 500: 'INTERNAL_ERROR' };
const err = (res, status, detail, reqPath) => {
  const st = status === 422 ? 409 : status;
  res.r = new Response(JSON.stringify({ type: 'about:blank', title: '', status: st, detail, code: CODES[status] ?? `HTTP_${status}`, instance: reqPath }), { status: st, headers: { 'Content-Type': 'application/problem+json' } });
};
const dow = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'];

function slotsFor(dateStr) {
  const d = new Date(`${dateStr}T00:00:00`);
  const sch = schedules.find((s) => s.dayOfWeek === dow[d.getDay()]);
  if (!sch) return [];
  const out = [];
  const [sh] = sch.startTime.split(':').map(Number); const [eh] = sch.endTime.split(':').map(Number);
  for (let t = new Date(d.setHours(sh, 0, 0, 0)); t.getHours() < eh; t = new Date(t.getTime() + 30 * 6e4)) {
    const end = new Date(t.getTime() + 30 * 6e4);
    const blocked = blocks.some((b) => parse(b.startDateTime) < end && parse(b.endDateTime) > t);
    const busy = appointments.some((a) => a.status !== 'CANCELADA' && parse(a.startTime) < end && parse(a.endTime) > t);
    out.push({ startTime: iso(t), endTime: iso(end), available: !blocked && !busy, reasonIfNotAvailable: blocked ? 'Horario bloqueado por descanso/actividad programada' : busy ? 'Horario ocupado por otra cita médica' : null });
  }
  return out;
}

/** Conflicto de horario con otra cita activa (el contrato pide 409). */
function overlaps(start, end, ignoreId = null) {
  const s = parse(start); const e = parse(end);
  return appointments.some((a) => a.id !== ignoreId && !['CANCELADA', 'NO_ASISTIO'].includes(a.status) && parse(a.startTime) < e && parse(a.endTime) > s);
}

async function api(req, res, url) {
  const p = url.pathname;
  const m = req.method;
  const cookie = req.token;
  const user = cookie ? sessionFromToken(cookie) : null;
  const body = req.json;
  const raw = req.raw;

  if (p === '/api/auth/login' && m === 'POST') {
    const u = USERS[(body?.username ?? '').trim().toLowerCase()];
    if (!u || u.password !== body.password.trim()) return err(res, 401, 'Credenciales incorrectas. Verifique su usuario y contraseña.', p);
    const s = sessionFromToken(`mock-${body.username.trim().toLowerCase()}`);
    return send(res, 200, { ...s, accessToken: `mock-${s.username}` });
  }
  // Igual que en cecamed-api, /system/info es público.
  if (p === '/api/system/info') return send(res, 200, { name: 'cecamed-backend (API simulada)', version: '0.1.0-SNAPSHOT', apiVersion: 'v1', googleCalendarEnabled: false });
  if (!user) return err(res, 401, 'La sesión no está iniciada o ha expirado', p);
  const medicoOnly = /^\/api\/(patients\/\d+\/(medical-record|consultations|documents)|consultations|documents)/.test(p);
  if (medicoOnly && user.role !== 'MEDICO') return err(res, 403, `El perfil ${user.roleDisplayName} no tiene permisos para esta operación`, p);

  let mm;
  if (p === '/api/auth/me') return send(res, 200, user);
  if (p === '/api/auth/logout') return send(res, 204);
  if ((mm = p.match(/^\/api\/documents\/(\d+)\/file$/))) { res.r = new Response(new Blob(['%PDF-1.4\n% Documento de ejemplo (API simulada)\n'], { type: 'application/pdf' }), { status: 200 }); return; }
  if (p === '/api/dashboard/summary') {
    const t = appointments.filter((a) => parse(a.startTime).toDateString() === today.toDateString());
    return send(res, 200, { appointmentsToday: t.length, waitingRoom: t.filter((a) => a.status === 'EN_SALA').length, completedToday: t.filter((a) => a.status === 'ATENDIDA').length, pendingToday: t.filter((a) => ['PROGRAMADA', 'CONFIRMADA'].includes(a.status)).length, activePatients: patients.filter((x) => x.active).length, todayAppointments: t });
  }
  if (p === '/api/patients' && m === 'GET') {
    const term = (url.searchParams.get('search') ?? '').toLowerCase(); const st = url.searchParams.get('status') ?? 'ACTIVE';
    let list = patients.filter((x) => !term || x.fullName.toLowerCase().includes(term) || (x.phone ?? '').includes(term));
    if (st === 'ACTIVE') list = list.filter((x) => x.active); if (st === 'INACTIVE') list = list.filter((x) => !x.active);
    return send(res, 200, list.sort((a, b) => a.lastName.localeCompare(b.lastName)));
  }
  if (p === '/api/patients' && m === 'POST') {
    if (patients.some((x) => x.firstName.toLowerCase() === body.firstName.toLowerCase() && x.lastName.toLowerCase() === body.lastName.toLowerCase() && x.birthDate === body.birthDate))
      return err(res, 422, 'Ya existe un paciente con los mismos nombres, apellidos y fecha de nacimiento. Revise el registro existente, incluidos los pacientes inactivos.', p);
    const np = { ...body, id: ++seq, fullName: `${body.firstName} ${body.lastName}`, age: now().getFullYear() - Number(body.birthDate.slice(0, 4)), active: true, medicalRecordNumber: `EXP-2026-${seq}00` };
    patients.push(np); records[np.id] = { id: np.id, patientId: np.id, recordNumber: np.medicalRecordNumber };
    return send(res, 201, np);
  }
  if ((mm = p.match(/^\/api\/patients\/(\d+)$/))) {
    const pt = patients.find((x) => x.id === +mm[1]); if (!pt) return err(res, 404, `Paciente no encontrado con id: '${mm[1]}'`, p);
    if (m === 'PUT') Object.assign(pt, body, { fullName: `${body.firstName} ${body.lastName}` });
    return send(res, 200, pt);
  }
  if ((mm = p.match(/^\/api\/patients\/(\d+)\/(activate|deactivate)$/))) { patients.find((x) => x.id === +mm[1]).active = mm[2] === 'activate'; return send(res, 204); }
  if ((mm = p.match(/^\/api\/patients\/(\d+)\/appointments$/))) return send(res, 200, appointments.filter((a) => a.patientId === +mm[1]).sort((a, b) => b.startTime.localeCompare(a.startTime)));
  if ((mm = p.match(/^\/api\/patients\/(\d+)\/medical-record$/))) { if (m === 'PUT') records[+mm[1]] = { ...records[+mm[1]], ...body }; return send(res, 200, records[+mm[1]]); }
  if ((mm = p.match(/^\/api\/patients\/(\d+)\/consultations$/))) return send(res, 200, consultations.filter((c) => c.patientId === +mm[1]).sort((a, b) => b.consultationDateTime.localeCompare(a.consultationDateTime)));
  if (p === '/api/consultations' && m === 'POST') {
    const v = body.vitalSigns; const bmi = v.weightKg && v.heightCm ? +(v.weightKg / (v.heightCm / 100) ** 2).toFixed(2) : null;
    const c = { ...body, id: ++seq, vitalSigns: { ...v, bmi }, patientFullName: patients.find((x) => x.id === body.patientId).fullName, attachedDocuments: [] };
    consultations.push(c); return send(res, 201, c);
  }
  if ((mm = p.match(/^\/api\/patients\/(\d+)\/documents$/))) {
    if (m === 'POST') { const d = { id: ++seq, patientId: +mm[1], fileName: 'x', originalFileName: (raw.match(/filename="([^"]+)"/) ?? [])[1] ?? 'archivo', fileType: 'application/pdf', documentType: (raw.match(/name="documentType"\r\n\r\n(\w+)/) ?? [])[1], fileSizeBytes: raw.length, description: (raw.match(/name="description"\r\n\r\n([^\r]+)/) ?? [])[1] ?? null, createdAt: iso(now()) }; documents.push(d); return send(res, 201, d); }
    return send(res, 200, documents.filter((d) => d.patientId === +mm[1]));
  }
  if ((mm = p.match(/^\/api\/documents\/(\d+)$/)) && m === 'DELETE') { documents.splice(documents.findIndex((d) => d.id === +mm[1]), 1); return send(res, 204); }
  if (p === '/api/appointments/available-slots') return send(res, 200, slotsFor(url.searchParams.get('date')));
  if (p === '/api/appointments' && m === 'GET') {
    const s = parse(url.searchParams.get('start')); const e = parse(url.searchParams.get('end')); const st = url.searchParams.get('status');
    return send(res, 200, appointments.filter((a) => parse(a.startTime) >= s && parse(a.startTime) <= e && (!st || a.status === st)).sort((a, b) => a.startTime.localeCompare(b.startTime)));
  }
  if (p === '/api/appointments' && m === 'POST') {
    if (parse(body.startTime) < now()) return err(res, 400, 'La hora de inicio debe ser en el presente o futuro', p);
    if (overlaps(body.startTime, body.endTime)) return err(res, 409, 'El horario seleccionado ya está ocupado por otra cita', p);
    const a = mkAppt(++seq, body.patientId, parse(body.startTime), 'PROGRAMADA', body.reasonForVisit); appointments.push(a); return send(res, 201, a);
  }
  if ((mm = p.match(/^\/api\/appointments\/(\d+)\/(reschedule|cancel|complete|status)$/))) {
    const a = appointments.find((x) => x.id === +mm[1]);
    if (!a) return err(res, 404, `Cita no encontrada con id: '${mm[1]}'`, p);
    if (mm[2] === 'reschedule' && overlaps(body.newStartTime, body.newEndTime, a.id)) return err(res, 409, 'El horario seleccionado ya está ocupado por otra cita', p);
    if (mm[2] === 'reschedule') Object.assign(a, { startTime: body.newStartTime, endTime: body.newEndTime, status: 'REPROGRAMADA' });
    if (mm[2] === 'cancel') Object.assign(a, { status: 'CANCELADA', cancellationReason: body.reason });
    if (mm[2] === 'complete') a.status = 'ATENDIDA';
    if (mm[2] === 'status') a.status = body.status;
    if (mm[2] === 'status' && body.status === 'EN_SALA') a.checkedInAt = iso(now());
    a.updatedAt = iso(now());
    return send(res, 200, a);
  }
  if (p === '/api/schedules') return send(res, 200, schedules);
  if (p === '/api/schedule-blocks' && m === 'GET') return send(res, 200, blocks);
  if (p === '/api/schedule-blocks' && m === 'POST') { const b = { ...body, id: ++seq, createdAt: iso(now()) }; blocks.push(b); return send(res, 201, b); }
  if ((mm = p.match(/^\/api\/schedule-blocks\/(\d+)$/))) { blocks.splice(blocks.findIndex((b) => b.id === +mm[1]), 1); return send(res, 204); }
  return err(res, 404, 'Recurso no encontrado', p);
}


export function installMockApi(apiBase: string) {
  const realFetch = window.fetch.bind(window);
  window.fetch = async (input, init = {}) => {
    const urlStr = typeof input === 'string' ? input : input.url;
    const url = new URL(urlStr, window.location.origin);
    if (!url.pathname.startsWith(apiBase + '/') ) return realFetch(input, init);
    // El simulador usa rutas /api/...; el cliente llama a <apiBase>/... (por defecto /api/v1).
    url.pathname = '/api' + url.pathname.slice(apiBase.length);
    const headers = init.headers || {};
    const auth = headers.Authorization || headers.authorization || '';
    let json = null, raw = '';
    if (init.body instanceof FormData) {
      const f = init.body.get('file');
      raw = `filename="${f?.name ?? 'archivo'}"\r\nname="documentType"\r\n\r\n${init.body.get('documentType')}\r\nname="description"\r\n\r\n${init.body.get('description') ?? ''}\r\n` + 'x'.repeat(f?.size ?? 0);
    } else if (typeof init.body === 'string') {
      json = JSON.parse(init.body);
      raw = init.body;
    }
    const req = { method: init.method ?? 'GET', json, raw, token: auth.startsWith('Bearer ') ? auth.slice(7) : null };
    const res = {};
    await new Promise((r) => setTimeout(r, 150));
    try {
      await api(req, res, url);
    } catch (e) {
      console.error(e);
      err(res, 500, 'Error interno', url.pathname);
    }
    return res.r;
  };
}
