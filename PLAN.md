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