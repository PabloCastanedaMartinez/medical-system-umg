import assert from 'node:assert/strict';
import { chromium } from 'playwright';

// Run only against a disposable backend/database: this test creates its own fixtures.
const base = process.env.E2E_BASE_URL ?? 'http://127.0.0.1:5173';
const admin = process.env.E2E_ADMIN_USERNAME;
const adminPassword = process.env.E2E_ADMIN_PASSWORD;
assert.ok(admin && adminPassword, 'Define E2E_ADMIN_USERNAME and E2E_ADMIN_PASSWORD for a disposable environment');
const marker = `e2e${Date.now()}`;
const password = 'Scheduling-E2e-482!';
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({ timezoneId: 'UTC' });
const page = await context.newPage();
page.setDefaultTimeout(15000);
const errors = [];
page.on('pageerror', (error) => errors.push(error.message));
let token;
async function api(path, body, method = 'POST') {
  const result = await page.request.fetch(`${base}/api/v1${path}`, {
    method, headers: { Authorization: `Bearer ${token}` }, data: body,
  });
  assert.ok(result.ok(), `API ${method} ${path}: ${result.status()} ${await result.text()}`);
  const text = await result.text(); return text ? JSON.parse(text) : undefined;
}
async function login(username, pass) {
  await page.goto(`${base}/login`);
  await page.locator('#username').fill(username);
  await page.locator('#password').fill(pass);
  await page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await page.waitForURL((url) => url.pathname !== '/login');
}
async function logout() {
  await page.getByRole('button', { name: 'Cerrar sesión', exact: true }).click();
  await page.waitForURL('**/login');
}
try {
  await login(admin, adminPassword);
  token = await page.evaluate(() => sessionStorage.getItem('cecamed.token'));
  const clinicDate = new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Guatemala', year: 'numeric', month: '2-digit', day: '2-digit' }).format(new Date());
  const tomorrow = new Date(`${clinicDate}T12:00:00Z`); tomorrow.setUTCDate(tomorrow.getUTCDate() + 1);
  const date = tomorrow.toISOString().slice(0, 10);
  const day = ['SUNDAY','MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY'][tomorrow.getUTCDay()];

  await page.getByRole('link', { name: 'Médicos', exact: true }).click();
  await page.locator('#doctor-name').fill(`Doctora ${marker}`);
  await page.locator('#doctor-specialty').fill('Medicina general');
  await page.locator('#doctor-user').fill(`doctor${marker}`);
  await page.locator('#doctor-password').fill(password);
  await page.locator('#hours-day-0').selectOption(day);
  await page.getByRole('button', { name: 'Guardar médico', exact: true }).click();
  await page.getByRole('row').filter({ hasText: `Doctora ${marker}` }).waitFor();
  const doctors = await api('/doctors', undefined, 'GET');
  const doctor = doctors.find((d) => d.username === `doctor${marker}`);
  assert.ok(doctor);
  for (let i = 2; i <= 3; i++) await api('/doctors', {
    fullName: `Doctor ${marker} ${i}`, specialty: i === 2 ? 'Pediatría' : 'Medicina interna', username: `doctor${marker}${i}`, password,
    schedules: [{ dayOfWeek: day, startTime: '08:00', endTime: '12:00', slotDurationMinutes: 30 }],
  });
  await page.getByRole('link', { name: 'Usuarios', exact: true }).click();
  await page.locator('#user-name').fill(`Recepción ${marker}`);
  await page.locator('#user-login').fill(`reception${marker}`);
  await page.locator('#user-password').fill(password);
  await page.getByRole('button', { name: 'Crear cuenta', exact: true }).click();
  await page.getByRole('row').filter({ hasText: `reception${marker}` }).waitFor();

  await page.getByRole('link', { name: 'Pacientes', exact: true }).click();
  await page.getByRole('button', { name: /Registrar paciente|Nuevo paciente/ }).first().click();
  let dialog = page.getByRole('dialog');
  await dialog.locator('#pf-first').fill('Paciente');
  await dialog.locator('#pf-last').fill(marker);
  await dialog.locator('#pf-birth').fill('2000-01-02');
  await dialog.locator('#pf-gender').selectOption('FEMENINO');
  await dialog.locator('#pf-phone').fill('55551234');
  await dialog.locator('#patient-allergies').fill('Sin alergias conocidas');
  await dialog.getByRole('button', { name: 'Guardar paciente', exact: true }).click();
  await dialog.waitFor({ state: 'hidden' });
  const patientRow = page.getByRole('row').filter({ hasText: marker });
  await patientRow.getByRole('button', { name: 'Crear acceso', exact: true }).click();
  dialog = page.getByRole('dialog');
  await dialog.locator('#portal-user').fill(`patient${marker}`);
  await dialog.locator('#portal-password').fill(password);
  await dialog.getByRole('button', { name: 'Crear acceso', exact: true }).click();
  await dialog.waitFor({ state: 'hidden' });
  const patients = await api(`/patients?search=${marker}`, undefined, 'GET');
  const patient = patients.content[0];
  await logout();

  await login(`patient${marker}`, password);
  await page.getByRole('heading', { name: 'Mis citas', exact: true }).waitFor();
  await page.getByRole('button', { name: 'Solicitar cita', exact: true }).click();
  dialog = page.getByRole('dialog');
  await dialog.locator('#ap-doctor').selectOption(String(doctor.id));
  await dialog.locator('#ap-date').fill(date);
  await dialog.locator('#ap-reason').fill(`Control ${marker}`);
  await dialog.getByRole('radio').first().waitFor();
  const available = await api(`/appointments/available-slots?date=${date}&doctorId=${doctor.id}`, undefined, 'GET');
  await api('/appointments', { doctorId: doctor.id, patientId: patient.id, startTime: available[0].startTime, endTime: available[0].endTime, reasonForVisit: 'Reserva simultánea' });
  await dialog.getByRole('button', { name: 'Agendar cita', exact: true }).click();
  await dialog.getByRole('alert').filter({ hasText: /ya tiene una cita|ocupado/ }).waitFor();
  await dialog.getByRole('radio').first().waitFor();
  assert.equal(await dialog.getByRole('radio').count(), 7, 'A 409 must reload available slots');
  await dialog.getByRole('button', { name: 'Agendar cita', exact: true }).click();
  await dialog.waitFor({ state: 'hidden' });
  await page.getByRole('row').filter({ hasText: `Control ${marker}` }).filter({ hasText: 'Pendiente de confirmación' }).waitFor();
  assert.equal(await page.getByRole('link', { name: 'Recepción', exact: true }).count(), 0);
  await logout();

  await login(`reception${marker}`, password);
  await page.getByRole('link', { name: 'Agenda', exact: true }).click();
  await page.getByLabel('Ir a fecha').fill(date);
  let row = page.getByRole('row').filter({ hasText: `Control ${marker}` });
  await row.getByRole('button', { name: 'Confirmar cita', exact: true }).click();
  await row.filter({ hasText: 'Confirmada' }).waitFor();
  await row.getByRole('button', { name: 'Reprogramar cita', exact: true }).click();
  dialog = page.getByRole('dialog');
  await dialog.getByRole('radio').last().click();
  await dialog.locator('#ap-resched').fill('Cambio solicitado por paciente');
  await dialog.getByRole('button', { name: 'Reprogramar', exact: true }).click();
  await dialog.waitFor({ state: 'hidden' });
  await row.filter({ hasText: 'Pendiente de confirmación' }).waitFor();
  await row.getByRole('button', { name: 'Cancelar cita', exact: true }).click();
  dialog = page.getByRole('dialog');
  await dialog.locator('#confirm-prompt-input').fill('Paciente cancela');
  await dialog.getByRole('button', { name: 'Cancelar cita', exact: true }).click();
  await dialog.waitFor({ state: 'hidden' });
  await row.filter({ hasText: 'Cancelada' }).waitFor();
  await logout();

  await login(`doctor${marker}`, password);
  await page.getByRole('link', { name: 'Agenda', exact: true }).click();
  await page.getByLabel('Ir a fecha').fill(date);
  await page.getByRole('row').filter({ hasText: `Control ${marker}` }).waitFor();
  assert.equal(await page.getByRole('button', { name: 'Cancelar cita', exact: true }).count(), 0);
  assert.equal(await page.getByRole('link', { name: 'Recepción', exact: true }).count(), 0);
  await logout();
  await login(`patient${marker}`, password);
  await page.getByRole('row').filter({ hasText: `Control ${marker}` }).filter({ hasText: 'Cancelada' }).waitFor();
  assert.deepEqual(errors, [], 'No browser runtime errors');
  console.log('PASS: 3 doctors; staff and patient access; booking conflict refresh; confirmation; rescheduling; cancellation; own agenda and history.');
} catch (error) {
  await page.screenshot({ path: '/tmp/cecamed-scheduling-e2e-failure.png', fullPage: true }).catch(() => {});
  throw error;
} finally { await browser.close(); }
