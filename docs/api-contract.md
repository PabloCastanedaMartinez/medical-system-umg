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
