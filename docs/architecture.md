# Arquitectura

## Contexto y decisión

CECAMed es una aplicación JavaFX que usa Spring Boot sin servidor web. El backend
nuevo conserva Java 21, PostgreSQL y Flyway, pero expone una API REST consumible
por un frontend desarrollado en otro proyecto. El código original se utiliza
como referencia funcional, sin acoplar sus módulos ni su base a este servicio.

Se adopta un monolito modular con puertos y adaptadores. Un único despliegue
permite transacciones locales y evita introducir infraestructura distribuida en
esta fase. Cuatro módulos Gradle separan el negocio del transporte y la persistencia.

## Responsabilidades y dependencias

| Módulo | Contenido | Dependencias de proyecto permitidas |
| --- | --- | --- |
| `cecamed-domain` | Modelos, objetos de valor y reglas de negocio | Ninguna |
| `cecamed-application` | Casos de uso, comandos/resultados y puertos de entrada/salida | Dominio |
| `cecamed-infrastructure` | Adaptadores JPA, mapeos, archivos, integraciones y configuración de casos de uso | Aplicación y dominio a través de aplicación |
| `cecamed-api` | Arranque, controladores, DTO HTTP, validación, errores, seguridad y OpenAPI | Aplicación e infraestructura para composición; dominio a través de aplicación |

El dominio y la aplicación se escriben en Java puro. La plataforma de versiones
de Spring Boot en Gradle es un catálogo de versiones; no añade librerías Spring
a sus classpaths de producción. Los controladores de negocio dependen de puertos
de entrada, y los casos de uso dependen de interfaces de salida definidas en aplicación.
Infraestructura implementa esas interfaces y declara los beans de composición.

Las entidades JPA se mantienen separadas del modelo de dominio y se convierten
en los adaptadores. Las entidades nunca son respuestas HTTP. Las transacciones
se definirán en los adaptadores que envuelvan casos de uso, para abarcar toda la
operación y mantener la aplicación sin anotaciones Spring.

El controlador técnico de versión consulta metadatos de compilación. No requiere
un caso de uso clínico ni acceso a persistencia.

## Organización por funcionalidad

Al migrar una funcionalidad se crean sus paquetes concretos, sin implementar
clases vacías de negocio de forma anticipada. Por ejemplo, para pacientes:

```text
domain/patient/                       # Patient, reglas y objetos de valor
application/patient/port/in/          # CreatePatientUseCase
application/patient/port/out/         # PatientRepositoryPort
application/patient/service/          # Implementación del caso de uso
application/patient/command/          # Entradas Java del caso de uso
application/patient/result/           # Resultados Java del caso de uso
infrastructure/persistence/patient/   # Entidades JPA, repositorios y adaptador
infrastructure/config/               # Beans y límites transaccionales
api/patient/                         # REST, requests/responses y conversión HTTP
```

Los DTO con Bean Validation pertenecen a API; las invariantes del negocio se
validan en dominio, incluso cuando el caso de uso se ejecuta fuera de HTTP.
Un módulo funcional no accederá directamente a los repositorios de otro; se
comunicará mediante los contratos de aplicación.

## Mapa del CECAMed existente

| Funcionalidad | Referencia original | Destino previsto | Estado |
| --- | --- | --- | --- |
| Identidad y acceso | Sesión y autenticación en `cecamed-ui` | `identity` | Implementado (Fase 2) |
| Pacientes | `PatientService`, `PatientIdentity`, repositorios y restricciones | `patient` | Implementado |
| Expedientes y consultas | `MedicalRecordService`, `MedicalConsultationService` | `clinical` | Implementado |
| Citas, horarios y bloqueos | `AppointmentService`, `DoctorScheduleService` | `scheduling` | Implementado |
| Recepción y sala de espera | Controladores de escritorio y estados de citas | `reception`, coordinado con `scheduling` | Implementado |
| Adjuntos | `PatientDocumentService` y almacenamiento local | `document` | Implementado: metadatos JPA y archivos locales |
| Inventario y kardex | `InventoryService` | `inventory` | Pendiente |
| Google Calendar | `cecamed-calendar-integration` | Adaptadores de `scheduling` | Pendiente |

## Decisiones transversales

- **Contrato:** JSON, rutas `/api/v1`, OpenAPI generado y errores Problem Details.
  Los DTO permiten evolucionar el esquema sin exponer la persistencia.
- **Seguridad (Fase 2 implementada):**
  - Autenticación Bearer stateless con token JWT HS256 (15 min) y refresh tokens opacos (7 días) rotativos persistidos mediante hash SHA-256.
  - Revocación de tokens en logout y detección de reutilización indebida de refresh tokens revocados (con invalidación de todas las sesiones del usuario).
  - Autorización por roles (`MEDICO`, `RECEPCION`, `ADMIN`) en rutas HTTP y `@PreAuthorize`. Principio de mínimo privilegio y denegación por defecto (`denyAll()`).
  - Protección contra fuerza bruta (bloqueo temporal tras 5 intentos fallidos) con mensajes genéricos para evitar enumeración.
  - Auditoría de seguridad persistida en base de datos de manera aislada (`REQUIRES_NEW`), sin registrar secretos ni contraseñas.
  - Sin sesiones HTTP, sin cookies y CSRF deshabilitado justificadamente.
- **CORS:** lista explícita de orígenes por entorno, sin credenciales de cookies.
  CSRF está desactivado bajo esta condición; si se adopta sesión/cookies, la
  configuración se revisará junto con el mecanismo de autenticación.
- **Datos:** base independiente, esquema `cecamed`, migraciones Flyway y
  validación Hibernate. No usar `ddl-auto=update` ni editar migraciones aplicadas.
  Flyway valida nombres y checksums; `clean`, baseline automático y los scripts
  de inicialización de Spring SQL están deshabilitados. `postgresTest` verifica
  el arranque sobre una base aislada; las pruebas normales usan H2 sin Flyway.
- **Perfiles:** `local` carga `.env` y construye la URL desde host, puerto y nombre
  cuando no hay `DB_URL`. `prod` requiere credenciales externas y no importa `.env`.
  `test` y `postgres-test` solo existen en recursos de pruebas.
- **Tiempo:** `Clock` UTC inyectable, Hibernate en UTC y conexiones del pool
  inicializadas con `SET TIME ZONE 'UTC'`. Los futuros eventos persistirán instantes
  y la agenda convertirá horarios usando una zona explícita, inicialmente la del
  CECAMed original (`America/Guatemala`).
- **Archivos:** puertos de persistencia y almacenamiento, metadatos JPA y archivos locales UUID. Acceso MEDICO, límite 15 MB, validación de firma/extensión PDF/PNG/JPEG y checksum SHA-256. La eliminación confirma primero el metadato inactivo; un fallo de disco permite reintentar DELETE. La carga limpia el archivo si falla el commit. No existe atomicidad distribuida entre base y disco ni conciliación automática tras una caída del proceso.
- **Operación:** salud sin detalles internos; readiness incluye la conexión a
  base de datos y liveness comprueba el proceso. Solo se expone Actuator health.
- **Errores:** códigos estables y mensajes sin trazas ni valores rechazados;
  excepciones técnicas inesperadas se registran en servidor.

## Orden de trabajo propuesto

1. Arquitectura y contrato técnico: esta entrega.
2. Identidad y acceso web: mecanismo de autenticación y permisos de médico,
   recepción y administración según los requisitos acordados.
3. Pacientes: primera migración SQL y caso de uso completo de extremo a extremo.
   Conservar la identidad por nombres, apellidos y nacimiento, sin identificación
   obligatoria, y comprobar normalización y duplicados concurrentes.
4. Expedientes, consultas, signos vitales y documentos.
5. Agenda, horarios, bloqueos, recepción y sincronización opcional de calendario.
6. Inventario, reportes y preparación del despliegue.

Cada etapa entregará contrato OpenAPI, implementación y pruebas relevantes.
La migración de datos históricos requiere un proceso explícito y separado del
arranque del backend; esta arquitectura no ejecuta dicha migración.

## Agenda médica implementada

El paquete `scheduling` mantiene el dominio y la aplicación independientes de Spring.
`SchedulingUseCase` expone los flujos y `SchedulingAdministrationUseCase` provisiona
identidad y perfiles. `SchedulingJdbcAdapter` implementa el puerto de persistencia
con consultas con JOIN y `TransactionTemplate`; no expone entidades en HTTP.

Cada mutación de agenda bloquea la fila del médico (`SELECT ... FOR UPDATE`) durante
la comprobación y la escritura. Jornadas y bloqueos usan el mismo bloqueo. PostgreSQL
agrega `EXCLUDE USING gist` sobre `doctor_id` y `tstzrange(start_time,end_time,'[)')`,
exceptuando citas canceladas. Esto protege incluso escrituras externas a la aplicación.
Las citas pendientes también reservan el intervalo; cancelar libera el espacio al commit.
Las inasistencias y atenciones conservan el intervalo histórico ocupado.

La jornada usa `DayOfWeek` y `LocalTime`; las reservas usan `Instant` y columnas
`TIMESTAMP WITH TIME ZONE`. La conversión a fecha de atención utiliza America/Guatemala.
Las reprogramaciones conservan una auditoría con actor, horario anterior y motivo, y
regresan a PENDIENTE. Los cambios requieren la versión actual y transiciones válidas.

Las asociaciones cuenta-médico y cuenta-paciente se guardan con claves foráneas y
unicidad. El portal deriva el paciente del usuario autenticado; nunca usa un ID enviado
por el navegador. La consulta de agenda de un médico deriva también su propio perfil.
