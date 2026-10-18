> Referencia histórica de pantallas. Para identidad, pacientes, expediente y consultas,
> el contrato vigente es [docs/api-contract.md](../../docs/api-contract.md).
> Las propuestas de agenda y documentos se revisarán en el incremento 2.

# Contrato de API que espera el frontend

Este documento es para el equipo del backend (`cecamed-api`). Enumera las rutas, entradas y salidas que el frontend ya consume. Si al implementarlas se decide un nombre o formato distinto, basta con avisar: los cambios en el frontend se concentran en `src/api/endpoints.ts` y `src/api/types.ts`.

Todas las rutas son relativas a **`/api/v1`**. Los errores siguen el formato *Problem Details* descrito en `docs/api-contract.md` del backend (`detail`, `code` y `errors[]`).

## Convenciones

- **Autenticación:** `Authorization: Bearer <token>` en todas las rutas salvo `POST /auth/login` y `GET /system/info`.
- **Fechas:** `YYYY-MM-DD` para fechas de calendario. Fecha y hora como instantes ISO 8601 con offset, igual que `docs/api-contract.md`. El frontend **envía** la hora de la clínica con su offset (`2026-10-15T09:30:00-06:00`), también en los filtros `start`/`end` y en los bloqueos de agenda. Al **recibir**, acepta cualquier offset (`…Z`, `…-06:00`) y **muestra** siempre la hora de `America/Guatemala`, sin importar la zona del equipo. Toda la conversión está en `utils/format.ts`.
- **Enumeraciones:** se usan los mismos valores del CECAMed original (`PROGRAMADA`, `EN_SALA`, `O_POSITIVO`, `MEDICO`, …).
- **Listas:** hoy se esperan arreglos simples. Si se pagina, avisar para adaptar.

## Identidad y acceso

| Método y ruta | Entrada | Respuesta |
| --- | --- | --- |
| `POST /auth/login` | `{ "username", "password" }` | `{ "accessToken", "username", "fullName", "role": "MEDICO" \| "RECEPCION", "roleDisplayName", "loginTime" }` · 401 si las credenciales son incorrectas |
| `GET /auth/me` | — | Igual que el login, sin `accessToken`. 401 si el token no es válido |
| `POST /auth/logout` | — | 204. Opcional: si no existe, el frontend solo borra el token |

Restricción por rol: el perfil **RECEPCION** no puede usar expediente, consultas ni documentos (403).

## Panel y sistema

| Método y ruta | Respuesta |
| --- | --- |
| `GET /system/info` | Ya existe. Opcionalmente añadir `googleCalendarEnabled` y `googleCalendarId` |
| `GET /dashboard/summary` | `{ appointmentsToday, waitingRoom, completedToday, pendingToday, activePatients, todayAppointments: Appointment[] }` |

## Pacientes

| Método y ruta | Entrada / parámetros | Respuesta |
| --- | --- | --- |
| `GET /patients?search=&status=ACTIVE\|INACTIVE\|ALL` | Búsqueda por nombre, apellido o teléfono | `Patient[]` ordenados por apellido |
| `GET /patients/{id}` | — | `Patient` |
| `POST /patients` | `PatientRequest` | 201 `Patient` · 409 si es duplicado (nombres, apellidos y fecha de nacimiento) |
| `PUT /patients/{id}` | `PatientRequest` | `Patient` |
| `PATCH /patients/{id}/activate` · `/deactivate` | — | 204 |
| `GET /patients/{id}/appointments` | — | `Appointment[]` del más reciente al más antiguo |

`PatientRequest`: `firstName`, `lastName`, `birthDate`, `gender` (`MASCULINO`/`FEMENINO`), `bloodType`, `phone`, `email`, `address`, `emergencyContactName`, `emergencyContactPhone`, `emergencyContactRelationship`, `notes`.

`Patient`: lo anterior más `id`, `fullName`, `age`, `active`, `medicalRecordNumber`, `createdAt`, `updatedAt`.

## Expediente clínico (solo MEDICO)

El frontend carga cada sección del expediente por separado: paciente, antecedentes, consultas, historial de citas y documentos. Si una ruta todavía no existe, solo esa pestaña muestra el error, así que el expediente se puede implementar antes que la agenda.

| Método y ruta | Entrada | Respuesta |
| --- | --- | --- |
| `GET /patients/{id}/medical-record` | — | `MedicalRecord` |
| `PUT /patients/{id}/medical-record` | `MedicalRecord` | `MedicalRecord` |
| `GET /patients/{id}/consultations` | — | `Consultation[]` del más reciente al más antiguo |
| `POST /consultations` | `ConsultationRequest` | 201 `Consultation` (con `vitalSigns.bmi` calculado) |

`MedicalRecord`: `recordNumber`, `allergies`, `currentMedications`, `pathologicalHistory`, `nonPathologicalHistory`, `familyHistory`, `surgicalHistory`, `traumaticHistory`, `generalObservations`, `gynecologicalObstetricHistory`, `waterGlassesPerDay`, `mealsPerDay`.

`ConsultationRequest`: `patientId`, `consultationDateTime`, `reason`, `symptoms`, `physicalExamination`, `diagnosis`, `icd10Code`, `treatmentPlan`, `privateNotes` y `vitalSigns` { `systolicPressure`, `diastolicPressure`, `heartRate`, `respiratoryRate`, `temperatureCelsius`, `weightKg`, `heightCm`, `oxygenSaturationPercentage` }.

## Documentos (solo MEDICO)

| Método y ruta | Entrada | Respuesta |
| --- | --- | --- |
| `GET /patients/{id}/documents` | — | `PatientDocument[]` |
| `POST /patients/{id}/documents` | `multipart/form-data`: `file`, `documentType`, `description` (opcional) | 201 `PatientDocument` · PDF, PNG o JPEG hasta 15 MB |
| `GET /documents/{id}/file` | — | El archivo, con su `Content-Type` |
| `DELETE /documents/{id}` | — | 204 |

`PatientDocument`: `id`, `originalFileName`, `fileType`, `documentType`, `fileSizeBytes`, `description`, `createdAt`.

## Agenda y recepción

| Método y ruta | Entrada / parámetros | Respuesta |
| --- | --- | --- |
| `GET /appointments?start=&end=&status=` | Inicio y fin del rango | `Appointment[]` ordenadas por hora |
| `GET /appointments/available-slots?date=YYYY-MM-DD` | — | `[{ startTime, endTime, available, reasonIfNotAvailable }]` |
| `POST /appointments` | `{ patientId, startTime, endTime, reasonForVisit, notes }` | 201 `Appointment` · 409 si el horario está ocupado |
| `PUT /appointments/{id}/reschedule` | `{ newStartTime, newEndTime, reason }` | `Appointment` |
| `POST /appointments/{id}/cancel` | `{ reason }` | `Appointment` |
| `POST /appointments/{id}/complete` | — | `Appointment` |
| `PATCH /appointments/{id}/status` | `{ status: CONFIRMADA \| EN_SALA \| ATENDIDA \| NO_ASISTIO }` | `Appointment`. Al pasar a `EN_SALA`, el backend registra `checkedInAt` |

`Appointment`: `id`, `patientId`, `patientFullName`, `patientPhone`, `startTime`, `endTime`, `status`, `reasonForVisit`, `cancellationReason`, `notes`, `googleSyncStatus`, `checkedInAt`, `createdAt`, `updatedAt`.

`checkedInAt` es la hora de llegada a sala (instante con offset). La sala de espera calcula el tiempo de espera con ese campo y no con `updatedAt`, porque cualquier otro cambio de la cita modifica `updatedAt`. Si llega `null`, la recepción muestra "Llegada sin registrar" y no cuenta tiempo.

## Horarios y bloqueos

| Método y ruta | Entrada | Respuesta |
| --- | --- | --- |
| `GET /schedules` | — | `[{ id, dayOfWeek: "MONDAY"…, startTime: "08:00:00", endTime, slotDurationMinutes, isActive }]` |
| `GET /schedule-blocks` | Por defecto, desde hace un mes hasta dentro de seis meses | `[{ id, title, blockType, startDateTime, endDateTime, allDay, reason }]` |
| `POST /schedule-blocks` | `{ title, blockType, startDateTime, endDateTime, allDay, reason }` | 201 |
| `DELETE /schedule-blocks/{id}` | — | 204 |

`blockType`: `VACACIONES`, `FERIADO`, `CAPACITACION`, `PERSONAL`, `MANTENIMIENTO`, `OTRO`.
