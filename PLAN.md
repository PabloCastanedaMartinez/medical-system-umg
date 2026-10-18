# Ejecución por incrementos

## Incremento 1: estabilizar lo existente — completado

Rama: `feature/incremento-1-estabilizacion`.

Implementado:
- Servicios clínicos registrados y patrones Spring Security válidos; MEDICO accede
  a contenido clínico, RECEPCION a pacientes y ADMIN a usuarios.
- Flyway V1–V5 sin versiones duplicadas. V4 conserva el SQL del antiguo expediente
  V2; V5 hace opcional la identificación, genera IDs de consultas, agrega CIE-10
  y completa expedientes de pacientes existentes.
- Alta de paciente y expediente inicial en una transacción; identificación opcional
  única cuando existe; validación de fecha civil en America/Guatemala.
- Búsqueda literal por nombre completo, identificación, teléfono, email y expediente,
  filtros y paginación; auditoría con usuario autenticado, IP y User-Agent reales.
- Timestamps UTC con precisión de microsegundos; versiones preservadas en todos
  los mapeos. Ediciones y activación/desactivación requieren la versión del cliente.
- Consultas con paciente válido/activo al crear, IDs y fechas del servidor, CIE-10,
  lectura y edición desde el frontend; conflictos devuelven 409 sin sobrescribir.
- Sesión frontend con rotación de refresh compartida entre peticiones, persistencia
  en la pestaña y revocación al salir. ADMIN reconocido y excluido de rutas clínicas.
- Contrato frontend adaptado a rutas anidadas, DTO clínico y listado paginado;
  eliminado filePath del tipo público de documentos.

Validación final (2026-10-18): 103 pruebas normales, 8 pruebas PostgreSQL y
4 pruebas del cliente aprobadas; compilación frontend real y simulada aprobada.

Condición de cierre: `./gradlew build`, pruebas del cliente y compilación frontend,
además del contrato login → paciente → expediente → consulta con PostgreSQL real.
El contrato se ejecuta tanto en pruebas normales (H2) como con Flyway/PostgreSQL,
incluyendo versiones obsoletas, escrituras simultáneas, permisos, auditoría,
validación, búsqueda, rotación y logout. Ver comandos y evidencia en README.

Revisión del análisis recibido:

| Área | Estado en esta entrega |
| --- | --- |
| Arquitectura y migraciones | Arranque integrado y esquema validados |
| Identidad | Login, refresh, logout, roles y auditoría integrados |
| Pacientes | Alta, lectura, búsqueda, edición y estado persistidos |
| Expediente | Creación inicial, lectura y edición con versión |
| Consultas básicas | Alta, listado, detalle y edición con versión |
| Firma/enmiendas, documentos, agenda y recepción | Incremento 2 |
| Inventario, reportes y operación | Incremento 3 |

Al iniciar se encontraron cambios **sin commit** de los incrementos 2 y 3 en el
worktree de inventario. Incluyen código de firma, documentos, agenda, inventario,
reportes y despliegue; por tanto, afirmar que no existe ninguna implementación
ya no describe ese directorio. No se incluyeron ni validaron en esta rama y no
se consideran entregados. Se conservaron íntegros en el worktree original.
Las futuras ramas deben reasignar sus migraciones V5/V6 después de esta V5,
antes de integrarlas; nunca editar checksums de migraciones ya aplicadas.

Limitaciones que siguen abiertas: simulador con `@ts-nocheck`, aceptación visual
completa del navegador, PostgreSQL 16 de Compose en CI y funcionalidades de los
incrementos siguientes. La validación local usa PostgreSQL 18.6 temporal.

## Incremento 2: completar la operación clínica

Firma y enmiendas, documentos, agenda, horarios, bloqueos, recepción, historial,
dashboard y pruebas de conflictos concurrentes y permisos.

## Incremento 3: inventario, reportes y preparación operativa

Definir e implementar reglas y pantallas, despliegue reproducible, respaldo y
restauración, almacenamiento persistente y verificaciones operativas. Calendar
es opcional y requiere concretar su alcance.

---

El plan anterior de fase 4 se conserva como referencia de requisitos pendientes;
no representa funcionalidades ya implementadas.

# Plan de Implementación - Fase 4: Expediente, consultas y documentos

## Visión General
Implementar el backend para el módulo de expediente clínico, consultas médicas y documentos del paciente siguiendo las convenciones existentes del proyecto y los requisitos especificados.

## Estructura de Paquetes
Se crearán tres nuevos módulos de feature siguiendo el patrón existente:
- `com.cecamed.backend.api.record` - Expediente médico
- `com.cecamed.backend.api.consultation` - Consultas médicas
- `com.cecamed.backend.api.document` - Documentos del paciente

Cada módulo seguirá la arquitectura por capas:
- API (controllers)
- Application (use cases, ports, models)
- Domain (entities, business rules)
- Infrastructure (persistence adapters, configuration)

## Componentes Clave

### 1. Expediente Médico (record)
**Dominio**: Entidad `MedicalRecord` con campos del contrato frontend y bloqueo optimista (`@Version`)
**Aplicación**: 
- Casos de uso: `GetMedicalRecordUseCase`, `UpdateMedicalRecordUseCase`
- Puertos: `MedicalRecordRepositoryPort`, `MedicalRecordReader`, `MedicalRecordWriter`
- DTOs: `MedicalRecordRequest`, `MedicalRecordResponse`
**Infraestructura**:
- Entidad JPA: `MedicalRecordJpaEntity`
- Adaptador repositorio: `MedicalRecordRepositoryAdapter`
**API**:
- Controlador: `MedicalRecordController`
- Endpoints:
  - `GET /patients/{patientId}/medical-record`
  - `PUT /patients/{patientId}/medical-record`

### 2. Consultas Médicas (consultation)
**Dominio**:
- Entidad `Consultation` con estado (`BORRADOR`, `FIRMADA`) y `@Version`
- Entidad `ConsultationAmendment` para enmiendas trazables
- Lógica de transición de estado en dominio
**Aplicación**:
- Casos de uso:
  - Listado paginado (resumen)
  - Obtener detalle
  - Crear borrador
  - Editar borrador
  - Firmar consulta
  - Crear enmienda
- Puertos: Puertos de repositorio, lectores/escritores de consulta
- DTOs: 
  - Resumen: `ConsultationSummary`
  - Detalle: `ConsultationResponse`
  - Solicitud: `ConsultationRequest`
  - Enmienda: `AmendmentRequest`
**Infraestructura**:
- Entidades JPA: `ConsultationJpaEntity`, `ConsultationAmendmentJpaEntity`
- Adaptadores repositorio
**API**:
- Controlador: `ConsultationController`
- Endpoints:
  - `GET /patients/{patientId}/consultations` (paginado)
  - `POST /patients/{patientId}/consultations` (crear)
  - `GET /patients/{patientId}/consultations/{id}` (detalle)
  - `PUT /patients/{patientId}/consultations/{id}` (editar borrador)
  - `POST /patients/{patientId}/consultations/{id}/sign` (firmar)
  - `POST /patients/{patientId}/consultations/{id}/amendments` (enmienda)

### 3. Documentos (document)
**Dominio**:
- Entidad `PatientDocument` (metadatos separados del contenido binario)
- Estado: `ACTIVE`, `INACTIVE` (desactivación lógica)
- Validación de contenido (magic bytes), checksum SHA-256
**Aplicación**:
- Casos de uso:
  - Listado paginado de metadatos
  - Subida de documento (multipart/form-data)
  - Descarga en streaming
  - Desactivación lógica
- Puertos:
  - `DocumentStoragePort` (almacenamiento abstracto)
  - `DocumentRepositoryPort` (metadatos)
  - `FileValidatorPort` (validación de archivos)
  - `ChecksumCalculatorPort` (SHA-256)
- DTOs: Solicitud de subida, respuesta de metadatos
**Infraestructura**:
- Entidad JPA: `PatientDocumentJpaEntity`
- Adaptador repositorio
- Adaptador almacenamiento sistema de archivos (configurable)
- Validadores de archivo (firma/magic bytes)
- Calculador de checksum
**API**:
- Controlador: `DocumentController`
- Endpoints:
  - `GET /patients/{patientId}/documents` (listado paginado)
  - `POST /patients/{patientId}/documents` (subida)
  - `GET /patients/{patientId}/documents/{id}/content` (descarga)
  - `POST /patients/{patientId}/documents/{id}/deactivate` (desactivación)

## Configuración
Nuevas propiedades en `application.yml`:
```yaml
cecamed:
  storage:
    type: filesystem
    filesystem:
      base-path: ${STORAGE_BASE_PATH:/tmp/cecamed-documents}
  document:
    allowed-types: ["ESTUDIO_LABORATORIO","IMAGEN_RAYOS_X","RECETA_MEDICA","INFORME_EXTERNO","CONSENTIMIENTO_INFORMADO","OTRO"]
    max-size-MB: 15
    virus-scan-enabled: false
```

## Autorización
Basado en la configuración de seguridad existente:
- `MEDICO`: Acceso completo a expediente, consultas y documentos
- `RECEPCION`: Sin acceso a contenido clínico (expediente y consultas) por defecto
- `ADMIN`: Según Fase 2 (probablemente sin acceso a datos clínicos)

Reglas por ruta en `SecurityConfiguration.java`:
```java
// Expediente
authorize.requestMatchers(HttpMethod.GET, "/api/v1/patients/**/medical-record").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.PUT, "/api/v1/patients/**/medical-record").hasRole("MEDICO")
// Consultas
authorize.requestMatchers(HttpMethod.GET, "/api/v1/patients/**/consultations").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.POST, "/api/v1/patients/**/consultations").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.GET, "/api/v1/patients/**/consultations/**").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.PUT, "/api/v1/patients/**/consultations/**").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.POST, "/api/v1/patients/**/consultations/**/sign").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.POST, "/api/v1/patients/**/consultations/**/amendments").hasRole("MEDICO")
// Documentos
authorize.requestMatchers(HttpMethod.GET, "/api/v1/patients/**/documents").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.POST, "/api/v1/patients/**/documents").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.GET, "/api/v1/patients/**/documents/**/content").hasRole("MEDICO")
authorize.requestMatchers(HttpMethod.POST, "/api/v1/patients/**/documents/**/deactivate").hasRole("MEDICO")
```

## Migraciones
Nuevos scripts Flyway en `db/migration`:
- `V4__create_medical_records_table.sql`
- `V5__create_consultations_table.sql`
- `V6__create_consultation_amendments_table.sql`
- `V7__create_patient_documents_table.sql`

Incluir:
- Tablas con columnas apropiadas
- Claves foráneas hacia pacientes (sin dependencia de Fase 5 para citas)
- Restricciones únicas (un expediente por paciente)
- Índices (paciente, fecha, estado)
- Columnas de versión para bloqueo optimista
- Constraints de checking para estados válidos

## Supuestos Pendientes de Validación
1. Campos exactos del expediente médico (usando tipo frontend MedicalRecord como base)
2. Matriz de permisos exacta para RECEPCION y ADMIN (validar con Fase 2)
3. Tipos de archivo permitidos y límites de tamaño (configurable)
4. Regla exacta de enmiendas (si requiere motivo obligatorio, etc.)
5. ¿Debe el expediente crearse automáticamente o bajo demanda?

## Riesgos Conocidos
1. Duplicación de esfuerzo si las suposiciones cambian significativamente
2. Complejidad en el manejo de transacciones para subida de documentos (atomicidad metadatos+archivo)
3. Rendimiento en listados grandes sin índices adecuados
4. Manejo correcto de errores de almacenamiento sin dejar huérfanos