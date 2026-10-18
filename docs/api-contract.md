# Integración con el frontend

## Recursos disponibles

Base local: `http://localhost:8080`. Las rutas de la API usan el prefijo `/api/v1`.

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| GET | `/api/v1/system/info` | Público | Información de versión y estado general |
| GET | `/actuator/health` | Público | Salud del proceso |
| GET | `/actuator/health/readiness` | Público | Disponibilidad con base de datos |
| GET | `/v3/api-docs` | Público (local) | Especificación OpenAPI v3 JSON |
| GET | `/swagger-ui/index.html` | Público (local) | Interfaz interactiva Swagger UI |
| POST | `/api/v1/auth/login` | Público | Inicio de sesión (devuelve Access y Refresh tokens) |
| POST | `/api/v1/auth/refresh` | Público | Rotación de Refresh Token |
| POST | `/api/v1/auth/logout` | Público / Auth | Cierre de sesión y revocación del Refresh Token |
| GET | `/api/v1/auth/me` | Autenticado | Perfil y rol del usuario de la sesión |
| POST | `/api/v1/users` | `ADMIN` | Creación de cuentas de usuario |
| GET | `/api/v1/users` | `ADMIN` | Listado paginado y filtrado de usuarios |
| GET | `/api/v1/users/{id}` | `ADMIN` | Detalle completo de un usuario |
| PUT | `/api/v1/users/{id}` | `ADMIN` | Actualización de nombre y rol |
| PATCH | `/api/v1/users/{id}/activate` | `ADMIN` | Activación de cuenta |
| PATCH | `/api/v1/users/{id}/deactivate` | `ADMIN` | Desactivación de cuenta y revocación de sesiones |
| POST | `/api/v1/users/{id}/reset-password` | `ADMIN` | Restablecimiento de contraseña y revocación de sesiones |

---

## Autenticación y Sesión (Bearer Token)

CECAMed utiliza arquitectura stateless con Bearer Token (sin cookies). El cliente envía en cada petición protegida:
```http
Authorization: Bearer <accessToken>
```

### 1. Iniciar sesión (`POST /api/v1/auth/login`)
**Solicitud:**
```json
{
  "username": "medico",
  "password": "Password123!"
}
```

**Respuesta exitosa (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "4Zk9xV8hQ1...",
  "username": "medico",
  "fullName": "Dra. Ana López",
  "role": "MEDICO",
  "roleDisplayName": "Médico",
  "loginTime": "2026-10-17T12:00:00Z"
}
```

### 2. Refrescar tokens (`POST /api/v1/auth/refresh`)
**Solicitud:**
```json
{
  "refreshToken": "4Zk9xV8hQ1..."
}
```

**Respuesta exitosa (`200 OK`):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1Ni...",
  "tokenType": "Bearer",
  "expiresInSeconds": 900,
  "refreshToken": "9jM3pL1aW2..."
}
```
*Nota:* Cada refresco invalida el token anterior y emite uno nuevo (**rotación estricta**). Reutilizar un token previo provoca la invalidación inmediata de todas las sesiones del usuario por detección de anomalías.

### 3. Cerrar sesión (`POST /api/v1/auth/logout`)
**Solicitud:**
```json
{
  "refreshToken": "9jM3pL1aW2..."
}
```
**Respuesta:** `204 No Content`.

### 4. Consultar sesión actual (`GET /api/v1/auth/me`)
Cabecera requerida: `Authorization: Bearer <accessToken>`.
**Respuesta exitosa (`200 OK`):**
```json
{
  "username": "medico",
  "fullName": "Dra. Ana López",
  "role": "MEDICO",
  "roleDisplayName": "Médico",
  "loginTime": "2026-10-17T12:00:00Z"
}
```

---

## Formato de Errores (Problem Details RFC 7807 / 9457)

Todos los errores de la API se responden con el content-type `application/problem+json`:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "El token de autenticación ha expirado.",
  "code": "TOKEN_EXPIRED",
  "instance": "/api/v1/patients"
}
```

### Catálogo de Códigos Estables (`code`)

| HTTP | Código (`code`) | Descripción / Causa |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Error en campos de la solicitud; incluye array `errors: [{field, message}]` |
| 400 | `PASSWORD_POLICY_VIOLATION` | Contraseña no cumple con los requisitos mínimos de seguridad (>= 12 caracteres) |
| 400 | `HTTP_400` | JSON malformado o cuerpo de petición ilegible |
| 401 | `AUTHENTICATION_REQUIRED` | Falta la cabecera `Authorization: Bearer` en una ruta protegida |
| 401 | `TOKEN_EXPIRED` | El token JWT expiró. El frontend emite el evento de sesión expirada |
| 401 | `INVALID_TOKEN` | Token JWT con firma inválida, manipulado o refresh token revocado |
| 401 | `INVALID_CREDENTIALS` | Usuario o contraseña incorrectos en login |
| 401 | `ACCOUNT_LOCKED` | Cuenta bloqueada temporalmente tras 5 intentos fallidos consecutivos |
| 401 | `ACCOUNT_INACTIVE` | Cuenta de usuario desactivada por un administrador |
| 403 | `ACCESS_DENIED` | Usuario autenticado pero sin rol suficiente para la ruta solicitada |
| 404 | `RESOURCE_NOT_FOUND` | Recurso solicitado no encontrado |
| 404 | `USER_NOT_FOUND` | Usuario no encontrado en administración |
| 409 | `USERNAME_ALREADY_EXISTS` | Intento de registrar un nombre de usuario que ya existe |
| 500 | `INTERNAL_ERROR` | Error interno del servidor sin exponer detalles sensibles |

## Pacientes, expediente y consultas — incremento 1

Los IDs son numéricos. `createdAt`, `updatedAt` y `consultationDate` son instantes
ISO-8601 UTC; la interfaz los muestra en America/Guatemala. `birthDate` es una
fecha civil `YYYY-MM-DD`, obligatoria y no futura en Guatemala.

| Ruta relativa a `/api/v1` | Contrato | Rol |
| --- | --- | --- |
| `GET /patients?search=&status=ACTIVE&page=0&size=20` | `{content, page, size, totalElements, totalPages}`; tamaño máximo 100; status ACTIVE/INACTIVE/ALL | MEDICO, RECEPCION |
| `POST /patients` | 201 + Location; crea también expediente vacío de forma atómica | MEDICO, RECEPCION |
| `GET /patients/{id}` | Paciente con `version`, timestamps y número de expediente | MEDICO, RECEPCION |
| `PUT /patients/{id}` | Datos completos editables + `version` leída | MEDICO, RECEPCION |
| `PATCH /patients/{id}/activate?version=0` | 204; idem `/deactivate` | MEDICO, RECEPCION |
| `GET /patients/{id}/medical-record` | Expediente con `version` | MEDICO |
| `PUT /patients/{id}/medical-record` | Campos editables + `version`; 200 con nueva versión | MEDICO |
| `GET /patients/{id}/consultations` | Array de consultas, más recientes primero | MEDICO |
| `POST /patients/{id}/consultations` | 201 + Location; ID y fechas generados por servidor | MEDICO |
| `GET /patients/{id}/consultations/{consultationId}` | Detalle; 404 si pertenece a otro paciente | MEDICO |
| `PUT /patients/{id}/consultations/{consultationId}` | Campos editables + `version`; 200 con nueva versión | MEDICO |

Alta mínima de paciente:

```json
{"firstName":"Ana","lastName":"López","birthDate":"2000-01-02","gender":"FEMENINO","bloodType":"DESCONOCIDO"}
```

`identificationNumber` es opcional al crear y se normaliza; cuando existe es único.
No se modifica con la edición demográfica. Los campos opcionales son teléfono,
email, dirección, contacto de emergencia y notas. No se acepta una versión ausente
al editar. La búsqueda interpreta `%` y `_` literalmente y combina el filtro de
estado con nombre completo, identificación, teléfono, email y número de expediente.

Alta mínima de consulta:

```json
{"consultationType":"GENERAL","doctorName":"Dra. López","reasonForVisit":"Control","diagnosis":"Evaluación general","icd10Code":"Z00","vitalSigns":"{\"heartRate\":70}"}
```

Campos adicionales: `symptoms`, `examinationFindings`, `treatmentPlan`,
`prescriptions`, `followUpInstructions`, `notes`. `vitalSigns` es texto que el
cliente web serializa como JSON de signos vitales; el adaptador frontend traduce
los nombres de presentación `reason`, `physicalExamination`, `privateNotes` e `id`
a `reasonForVisit`, `examinationFindings`, `notes` y `consultationId` del contrato.
La respuesta añade `patientId`, `consultationDate`, `createdAt`, `updatedAt` y
`version`. El paciente debe existir y estar activo al crear una consulta.

PUT reemplaza los campos editables; un valor opcional nulo borra ese dato.
Los IDs, fecha de consulta y fecha de creación no cambian al editar. La versión
leída se envía en cada edición y cambio de estado: 400 si falta, 409 si quedó
obsoleta, incluso con peticiones concurrentes. Se devuelve la nueva versión
después de persistir. El cliente conserva el formulario al recibir un conflicto;
no debe reintentar automáticamente sobrescribiendo con la versión nueva.

Los permisos se comprueban en el servidor. ADMIN no tiene acceso clínico.
Firma y enmiendas siguen pendientes. Documentos y agenda están implementados.
`filePath` y la clave de almacenamiento no forman parte del contrato público.
Las descargas del frontend usan el cliente HTTP autenticado.

## Médicos, disponibilidad y citas (API implementada)

Todas las rutas siguientes parten de `/api/v1`. Fechas de calendario: `YYYY-MM-DD`;
horas de jornada: `HH:mm`; reservas: ISO 8601 con zona (`Z` o `-06:00`).

| Método y ruta | Acceso | Entrada / resultado |
| --- | --- | --- |
| GET `/doctors` | Personal y paciente | Médicos activos con ID, nombre y especialidad |
| POST `/doctors` | RECEPCION, ADMIN | fullName, specialty, username, password opcional para cuenta existente, schedules |
| PUT `/doctors/{id}` | RECEPCION, ADMIN | Datos, horarios y version actual |
| GET `/schedules?doctorId=` | Personal y paciente | Bloques por médico: dayOfWeek, startTime, endTime, slotDurationMinutes |
| GET `/appointments/available-slots?doctorId=&date=` | Personal y paciente | Solo horarios futuros libres; cada uno contiene doctorId, startTime, endTime |
| GET `/appointments?start=&end=&doctorId=&status=` | Personal | Agenda por rango; MEDICO solo puede consultar su propio perfil |
| GET `/doctors/me/agenda` | MEDICO | Agenda del día en America/Guatemala |
| POST `/appointments` | RECEPCION, ADMIN | doctorId, patientId, startTime, endTime, reasonForVisit, notes opcional |
| PUT `/appointments/{id}/reschedule` | RECEPCION, ADMIN | newStartTime, newEndTime, reason, version |
| PATCH `/appointments/{id}/status` | RECEPCION, ADMIN | status, version |
| POST `/appointments/{id}/cancel` | RECEPCION, ADMIN | reason, version |
| GET `/patients/{id}/appointments` | Personal | Historial con médico, paciente, fecha, motivo y estado |
| POST `/patients/{id}/portal-access` | RECEPCION, ADMIN | username, password; crea cuenta PACIENTE asociada |
| GET `/portal/me` | PACIENTE | Perfil del paciente autenticado |
| GET `/portal/appointments` | PACIENTE | Historial propio |
| POST `/portal/appointments` | PACIENTE | doctorId, startTime, endTime, reasonForVisit; paciente derivado del token |
| GET `/schedule-blocks` | RECEPCION, ADMIN | Bloqueos, opcionalmente por doctorId |
| POST `/schedule-blocks` | RECEPCION, ADMIN | doctorId, title, startDateTime, endDateTime, blockType, allDay, reason |
| DELETE `/schedule-blocks/{id}` | RECEPCION, ADMIN | Quita el bloqueo |
| GET `/dashboard/summary` | Personal | Resumen diario; para MEDICO solo su agenda |

Un bloque de jornada debe contener un número entero de citas de 5 a 240 minutos;
se admiten varios bloques diarios sin solaparse. Para reagendar, el personal puede
enviar `excludingId` al consultar disponibilidad. Los pacientes no pueden excluir citas.

Estados: PENDIENTE → CONFIRMADA → EN_SALA → ATENDIDA. PENDIENTE o CONFIRMADA pueden
pasar a NO_ASISTIO o CANCELADA; EN_SALA puede cancelarse. Estados terminales no pueden
reactivarse. Reprogramar una cita pendiente o confirmada la devuelve a PENDIENTE.

HTTP 409 `APPOINTMENT_SLOT_OCCUPIED`: espacio ocupado. HTTP 409 `STALE_VERSION`:
versión desactualizada. HTTP 400: intervalo inválido, pasado o fuera de jornada.
HTTP 403: rol o pertenencia incorrectos. Una cancelación libera su espacio.

## Documentos clínicos (implementado)

Todas las rutas requieren MEDICO. Recepción, administración y pacientes reciben
403; sin sesión se devuelve 401.

| Método y ruta relativa a /api/v1 | Resultado |
| --- | --- |
| GET /patients/{patientId}/documents | Lista de documentos activos, más recientes primero. |
| POST /patients/{patientId}/documents | Multipart: file, documentType, description opcional. 201 y Location de descarga. |
| GET /documents/{id}/file | Bytes autenticados, Content-Type detectado, attachment y no-store. |
| DELETE /documents/{id} | Desactiva el metadato y elimina los bytes. 204; reintento idempotente. |
| GET /patients/{patientId}/documents/{id}/content | Descarga equivalente, validando pertenencia al paciente. |
| POST /patients/{patientId}/documents/{id}/deactivate | Eliminación equivalente, validando pertenencia al paciente. |

Respuesta: id, patientId, consultationId (null), fileName, originalFileName,
fileType, documentType, fileSizeBytes, checksumSha256, description, createdAt
y updatedAt. Fechas UTC. Sin asociación a consulta en esta versión.

Tipos: ESTUDIO_LABORATORIO, IMAGEN_RAYOS_X, RECETA_MEDICA, INFORME_EXTERNO,
CONSENTIMIENTO_INFORMADO y OTRO. PDF, PNG o JPEG no vacío, hasta 15 MiB;
se detecta la firma sin confiar en el MIME enviado y se exige extensión coherente.
Descripción: máximo 2000 caracteres. Esta comprobación no es un análisis antivirus
ni validación estructural completa del documento.

400 para entradas inválidas; 413 para exceso de tamaño; 404 para paciente/documento
inexistente, documento inactivo o de otro paciente; 409 al cargar para un paciente
inactivo. Errores Problem Details. La eliminación conserva el metadato inactivo y
borra el archivo físico. Un fallo de disco permite reintentar DELETE con el mismo ID.
