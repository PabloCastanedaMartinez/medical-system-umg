# Integración con el frontend

## Recursos disponibles

Base local: `http://localhost:8080`. Las rutas funcionales usarán `/api/v1`.
El backend no necesita tener acceso al repositorio del frontend.

| Método | Ruta | Estado en esta etapa |
| --- | --- | --- |
| GET | `/api/v1/system/info` | Disponible sin autenticación |
| GET | `/actuator/health` | Disponible sin autenticación |
| GET | `/actuator/health/liveness` | Disponible sin autenticación |
| GET | `/actuator/health/readiness` | Disponible sin autenticación; incluye disponibilidad de base |
| GET | `/v3/api-docs` | Contrato generado; habilitado en perfil local |
| GET | `/swagger-ui/index.html` | Explorador de API; habilitado en perfil local |

Ejemplo de `GET /api/v1/system/info`:

```json
{
  "name": "cecamed-backend",
  "version": "0.1.0-SNAPSHOT",
  "apiVersion": "v1"
}
```

Ejemplo de salud: `{"status":"UP"}`. Un problema de disponibilidad puede devolver
HTTP 503; no se exponen detalles de conexión a la base de datos.

## CORS y autenticación

En local se permiten `http://localhost:5173` y `http://localhost:3000`. Para otro
origen, establecer `CORS_ALLOWED_ORIGINS` y reiniciar. Se permiten las cabeceras
`Authorization`, `Content-Type` y `Accept`. No se usan cookies de sesión.

Las rutas de negocio todavía no están implementadas y se encuentran bloqueadas
por la política de seguridad: una llamada anónima recibe HTTP 401. Aún no existe
un endpoint de login ni un emisor/validador de tokens. El frontend puede comenzar
probando conectividad, CORS y metadatos; el contrato de autenticación y los de
negocio se incorporarán en las siguientes etapas.

## Formato de errores

Los errores de controladores y seguridad usan `application/problem+json`, basado
en Problem Details. Ejemplo de una ruta protegida:

```json
{
  "type": "about:blank",
  "title": "Unauthorized",
  "status": 401,
  "detail": "Se requiere autenticación.",
  "code": "AUTHENTICATION_REQUIRED"
}
```

Los errores procesados por controladores incluyen `instance` con la ruta, sin
parámetros de consulta. Los errores de validación incluirán un array `errors`
con objetos `field` y `message`. El frontend debe usar `code` para sus decisiones;
el texto de `detail` y `message` puede evolucionar.

| HTTP | Código | Situación |
| --- | --- | --- |
| 400 | `VALIDATION_ERROR` | Campos inválidos en el cuerpo de una solicitud |
| 400 | `HTTP_400` | JSON ilegible u otro error HTTP de entrada |
| 401 | `AUTHENTICATION_REQUIRED` | Falta autenticación |
| 403 | `ACCESS_DENIED` | Principal autenticado sin autorización |
| 404 | `RESOURCE_NOT_FOUND` | Recurso no encontrado por un caso de uso |
| 409 | Código definido por la regla | Conflicto de negocio |
| 500 | `INTERNAL_ERROR` | Error técnico sin detalles internos |

El filtro CORS puede rechazar un origen con HTTP 403 antes de llegar a los
controladores; ese rechazo puede ser texto y no sigue el formato anterior.

## Convenciones para próximas funcionalidades

- DTO explícitos para entrada y salida; nunca serializar entidades JPA.
- Fechas de calendario en `YYYY-MM-DD`, instantes en ISO 8601 con zona/offset.
- Los contratos de listas paginadas, filtros, autenticación y rutas de negocio
  se fijarán al desarrollar la primera funcionalidad que los necesite.
- Ninguna ruta de negocio prevista debe considerarse disponible hasta aparecer
  en OpenAPI y contar con implementación y pruebas.

Para compartir el contrato actual con el equipo del frontend:

```bash
curl http://localhost:8080/v3/api-docs -o openapi.json
```
