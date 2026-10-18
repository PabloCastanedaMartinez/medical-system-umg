# CECAMed

Sistema web de gestión clínica con una API REST en Java y una interfaz en React. Reúne pacientes, expedientes, consultas, agenda por médico, recepción y acceso de pacientes a sus citas. El proyecto toma como referencia funcional el CECAMed de escritorio (JavaFX); utiliza una base de datos independiente.

## Funcionalidades y alcance

| Área | Implementación actual |
| --- | --- |
| Identidad | Inicio de sesión, JWT, renovación rotatoria, cierre de sesión, administración de usuarios, bloqueo por intentos fallidos y auditoría. |
| Pacientes | Búsqueda y paginación, registro, edición, activación/desactivación y control de versiones. |
| Expediente clínico | Antecedentes y consultas con diagnóstico, CIE-10, tratamiento y signos vitales; acceso exclusivo del médico. |
| Médicos | Registro de médico y cuenta asociada, especialidad y jornadas con duración de turnos. |
| Agenda | Disponibilidad por médico, reserva, confirmación, reprogramación, cancelación e historial. Protección frente a reservas superpuestas. |
| Recepción | Llegada a sala de espera, tiempo de espera y cambios de estado de las citas. |
| Portal del paciente | Cuenta vinculada al paciente, solicitud de citas y consulta de su propio historial. |
| Interfaz | Navegación por rol, tema claro/oscuro, diseño adaptable y simulador de API para demostraciones. |
| Pendiente | Inventario/kardex e integración con Google Calendar. |

Los adjuntos clínicos permiten cargar, listar, descargar y eliminar PDF, PNG y JPEG de hasta 15 MB. Los metadatos se guardan en PostgreSQL y los archivos en un directorio persistente configurable. Solo MEDICO tiene acceso.

## Tecnologías

- **Backend:** Java 21, Spring Boot 3.5.16, Spring Security, Bean Validation y Springdoc OpenAPI.
- **Datos:** PostgreSQL 16, Spring Data JPA, JDBC y migraciones Flyway.
- **Construcción:** Gradle Wrapper 8.14.3 con Kotlin DSL y cuatro módulos Java.
- **Frontend:** React 18.3, TypeScript 5.6, Vite 6.4, React Router 7, Lucide y CSS propio.
- **Pruebas:** JUnit 5, AssertJ, Mockito, Spring Boot Test, H2, PostgreSQL, Node Test Runner y Playwright.

Las versiones exactas están en los archivos Gradle y en [package.json](cecamed-frontend/package.json) y su lockfile.

## Estructura y arquitectura

El backend es un monolito modular con puertos y adaptadores. Dominio y aplicación no dependen de Spring; infraestructura implementa persistencia y seguridad, y API compone los servicios y expone HTTP.

```text
cecamed-backend/
├── cecamed-domain/           # Modelos y reglas de negocio; sin dependencias de otros módulos
├── cecamed-application/      # Casos de uso, DTO y puertos; depende de dominio
├── cecamed-infrastructure/   # Adaptadores JPA/JDBC, seguridad, configuración y Flyway
├── cecamed-api/              # Aplicación Spring Boot, controladores y seguridad HTTP
├── cecamed-frontend/         # Aplicación React, cliente HTTP, simulador y pruebas
├── docs/                    # Arquitectura y contratos
├── gradle/wrapper/           # Distribución Gradle fijada para el proyecto
├── compose.yaml             # PostgreSQL local con volumen persistente
├── .env.example             # Configuración de ejemplo para desarrollo
└── settings.gradle.kts      # Módulos Java incluidos
```

En el frontend, `src/api` concentra transporte y contratos; `src/auth` gestiona sesión y permisos; `src/pages` contiene las pantallas; `src/components`, `src/theme` y `src/utils` contienen elementos compartidos. El frontend se instala y compila con npm, de forma independiente a Gradle.

## Requisitos

- BellSoft Liberica JDK **21**, disponible mediante `JAVA_HOME` y `PATH`. Gradle exige el proveedor BellSoft.
- Node.js **20 o superior** y npm.
- Docker con Compose, o una instancia PostgreSQL 16 preparada por separado.
- Acceso a los repositorios de dependencias durante la primera instalación.

Los comandos siguientes parten de la raíz del repositorio y usan Bash. En Windows se puede usar `gradlew.bat` en lugar de `./gradlew`; la asignación de variables de entorno debe adaptarse al shell.

Para la instalación de BellSoft disponible en este equipo:

```bash
export JAVA_HOME="$HOME/.jdks/liberica-full-21.0.12.1"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

En otros equipos, ajustar la ruta a su instalación de Liberica 21.
IntelliJ debe usar ese JDK también como Gradle JVM.

## Inicio rápido con API real

### 1. Configurar el entorno

```bash
cp .env.example .env
```

Editar `.env` antes de arrancar:

- Establecer `DB_PASSWORD` para PostgreSQL.
- Configurar `JWT_SECRET` con una clave propia de al menos 32 caracteres.
- Configurar `BOOTSTRAP_ADMIN_USERNAME` y `BOOTSTRAP_ADMIN_PASSWORD` para crear la primera cuenta administradora; la contraseña debe tener al menos 12 caracteres.

El perfil `local`, activo por defecto, lee el archivo `.env` de la raíz. La tarea `bootRun` usa ese directorio de trabajo. Docker Compose también lee ese archivo.

### 2. Iniciar PostgreSQL y el backend

```bash
docker compose up -d postgres
docker compose ps
./gradlew :cecamed-api:bootRun
```

PostgreSQL se publica en `127.0.0.1:5433` y la API en `http://localhost:8080`. Flyway crea y migra el esquema `cecamed` al iniciar; Hibernate valida su correspondencia con las entidades.

El administrador se crea si el nombre configurado aún no existe. El bootstrap no actualiza la contraseña de una cuenta existente. Se pueden retirar ambas variables de bootstrap después de crear la cuenta.

### 3. Iniciar el frontend en otra terminal

```bash
cd cecamed-frontend
npm ci
npm run dev
```

Abrir **http://localhost:5173** e ingresar con las credenciales de bootstrap configuradas. Vite reenvía `/api` a `http://localhost:8080`.

Desde administración se pueden crear cuentas de personal; la pantalla **Médicos** registra el perfil, la cuenta y sus horarios. Para habilitar el portal, registrar un paciente y usar **Crear acceso** desde pacientes con un rol autorizado.

### 4. Comprobar disponibilidad

| Recurso | URL local |
| --- | --- |
| Información del servicio | http://localhost:8080/api/v1/system/info |
| Salud | http://localhost:8080/actuator/health |
| Liveness | http://localhost:8080/actuator/health/liveness |
| Readiness, incluida la base de datos | http://localhost:8080/actuator/health/readiness |
| Swagger UI, perfil local | http://localhost:8080/swagger-ui/index.html |
| OpenAPI, perfil local | http://localhost:8080/v3/api-docs |

`docker compose stop` detiene PostgreSQL conservando los datos. El volumen `postgres_data` mantiene la información entre arranques.

## Demostración sin backend

```bash
cd cecamed-frontend
npm ci
npm run dev:mock
```

| Perfil | Usuario | Contraseña de demostración |
| --- | --- | --- |
| Administración | `admin` | `admin123` |
| Médico | `medico` | `medico123` |
| Recepción | `recepcion` | `recepcion123` |
| Paciente | `paciente` | `paciente123` |

Estas cuentas pertenecen exclusivamente al simulador. Los cambios se mantienen en memoria y se reinician al recargar. El simulador contiene `@ts-nocheck` y no sustituye las pruebas con PostgreSQL ni las comprobaciones de autorización del backend.

## Configuración

### Backend y PostgreSQL

| Variable | Uso / valor predeterminado |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` por defecto; `prod` para configuración de producción. |
| `DB_URL` | URL JDBC completa; obligatoria en `prod`. En local prevalece sobre host, puerto y nombre. |
| `DB_HOST`, `DB_PORT`, `DB_NAME` | En local: `localhost`, `5433`, `cecamed_backend`. |
| `DB_USERNAME` | Usuario; en local, `cecamed_backend`. Obligatorio en `prod`. |
| `DB_PASSWORD` | Contraseña obligatoria para API y Compose. |
| `DOCUMENTS_DIRECTORY` | Directorio de adjuntos: `uploads/documents`, relativo al directorio de ejecución. Usar una ruta persistente absoluta en producción. |
| `SERVER_PORT` | Puerto HTTP: `8080`. |
| `SERVER_ADDRESS` | `127.0.0.1` en local; `0.0.0.0` en producción. |
| `CORS_ALLOWED_ORIGINS` | Orígenes separados por comas. Local permite `http://localhost:5173` y `http://localhost:3000`; producción no permite orígenes por defecto. |
| `JWT_SECRET` | Clave de firma HS256; obligatoria en producción. Local tiene un valor de desarrollo. |
| `JWT_ACCESS_TOKEN_EXPIRATION_MINUTES` | Vigencia de acceso: `15`. |
| `JWT_REFRESH_TOKEN_EXPIRATION_DAYS` | Vigencia de renovación: `7`. |
| `BRUTE_FORCE_MAX_ATTEMPTS` | Intentos fallidos antes del bloqueo: `5`. |
| `BRUTE_FORCE_LOCKOUT_MINUTES` | Duración del bloqueo: `15`. |
| `BOOTSTRAP_ADMIN_USERNAME`, `BOOTSTRAP_ADMIN_PASSWORD` | Creación opcional de la cuenta inicial. |
| `BOOTSTRAP_ADMIN_FULL_NAME` | Nombre mostrado para la cuenta inicial. |

El perfil `prod` no importa `.env`: las variables deben suministrarse al proceso. Compose inicia únicamente PostgreSQL; no construye ni inicia la API o el frontend.

### Frontend

| Variable | Uso |
| --- | --- |
| `CECAMED_API_URL` | Destino del proxy de desarrollo; por defecto `http://localhost:8080`. |
| `VITE_API_BASE_URL` | Prefijo HTTP del cliente; por defecto `/api/v1`. Puede definirse en `.env.local` o durante la compilación. |
| `VITE_USE_MOCK` | Activa el simulador con `true`; también se activa mediante `--mode mock`. |

Ejemplo con otro destino para el proxy:

```bash
CECAMED_API_URL=http://localhost:8081 npm run dev
```

La configuración actual de Vite lee `CECAMED_API_URL` y `VITE_USE_MOCK` desde el entorno del proceso. Para estas variables, usar la terminal; para simulación, preferir `npm run dev:mock`. Las variables `VITE_*` se incorporan al cliente al compilar y no deben contener secretos.

## Roles y acceso

| Perfil | Acceso principal |
| --- | --- |
| `ADMIN` | Usuarios, pacientes, médicos, agenda, recepción y bloqueos. No tiene acceso al expediente clínico. |
| `RECEPCION` | Pacientes, médicos y jornadas, gestión de citas, recepción, bloqueos y creación de accesos al portal. |
| `MEDICO` | Pacientes, expedientes y consultas; agenda propia y consulta de historial de citas. No modifica reservas mediante las rutas de gestión de agenda. |
| `PACIENTE` | Perfil e historial propios, consulta de disponibilidad y solicitud de citas. |

La API aplica autorización aunque se invoque directamente. En el portal, el paciente se deriva de la cuenta autenticada; la agenda del médico se limita a su perfil asociado.

Rutas principales del frontend: `/login`, `/`, `/pacientes`, `/expediente/:patientId`, `/agenda`, `/recepcion`, `/medicos`, `/usuarios`, `/mis-citas`, `/pacientes/:patientId/citas` y `/configuracion`. Configuración está disponible para usuarios autenticados; la pestaña de bloqueos se limita a recepción y administración.

## Contrato HTTP y reglas de datos

Todas las rutas de negocio usan el prefijo **`/api/v1`**.

| Grupo | Rutas representativas |
| --- | --- |
| Sesión | `/auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/me` |
| Usuarios | `/users`, `/users/{id}`, activación, desactivación y restablecimiento de contraseña |
| Pacientes | `/patients`, `/patients/{id}`, activación y desactivación |
| Clínica | `/patients/{patientId}/medical-record`, `/patients/{patientId}/consultations` |
| Médicos y horarios | `/doctors`, `/doctors/me/agenda`, `/schedules`, `/schedule-blocks` |
| Citas | `/appointments`, `/appointments/available-slots`, `/appointments/{id}/reschedule`, `/appointments/{id}/status`, `/appointments/{id}/cancel` |
| Portal | `/patients/{id}/portal-access`, `/portal/me`, `/portal/appointments` |
| Panel | `/dashboard/summary` |

- **Sesión:** Bearer JWT sin sesión HTTP ni cookies de autenticación. Los refresh tokens son rotatorios y se almacenan mediante hash en el backend. El navegador conserva ambos tokens en `sessionStorage` y comparte una renovación entre solicitudes concurrentes que reciben 401.
- **Errores:** Problem Details con `code` estable, `detail` y errores de validación cuando corresponda. El cliente debe decidir por código, no por texto del mensaje.
- **Concurrencia:** las ediciones que requieren `version` deben enviar la versión leída y gestionar el conflicto sin sobrescribir cambios recientes.
- **Tiempo:** persistencia y conexiones en UTC; agenda e interfaz usan `America/Guatemala`. Los instantes viajan en ISO 8601 con offset. La fecha de consulta clínica la asigna el servidor.
- **Reservas:** las mutaciones bloquean la fila del médico. PostgreSQL refuerza la exclusión de intervalos superpuestos mediante GiST; las citas canceladas liberan el horario.

El recorrido habitual de una cita es `PENDIENTE → CONFIRMADA → EN_SALA → ATENDIDA`. También existen `CANCELADA` y `NO_ASISTIO`, según las transiciones permitidas. Reprogramar una cita pendiente o confirmada la devuelve a `PENDIENTE` y registra auditoría.

## Base de datos y migraciones

Las migraciones están en [cecamed-infrastructure/src/main/resources/db/migration](cecamed-infrastructure/src/main/resources/db/migration):

| Versión | Contenido |
| --- | --- |
| V1 | Usuarios, refresh tokens y auditoría de seguridad. |
| V2 | Pacientes. |
| V3 | Consultas. |
| V4 | Expedientes. |
| V5 | Estabilización de datos clínicos, identificación opcional y CIE-10. |
| V6 | Médicos, jornadas, citas, bloqueos, cuentas del portal y auditoría de agenda. |
| V7 | Metadatos, checksum SHA-256 y estado de adjuntos por paciente. |

V6 utiliza la extensión PostgreSQL `btree_gist`; la instancia debe permitir su instalación o tenerla preparada. Flyway valida nombres y checksums; `clean` y el baseline automático están deshabilitados. Hibernate usa `ddl-auto=validate`.

Agregar una nueva migración para cada cambio de esquema, sin editar las ya aplicadas. Para instalaciones que hayan aplicado la antigua versión duplicada del expediente, consultar las [notas de migración](cecamed-infrastructure/src/main/resources/db/migration/README.md) antes de actualizar. No existe una migración automática desde la base de la aplicación de escritorio.

## Pruebas y compilación

### Backend

Desde la raíz:

```bash
./gradlew check
./gradlew build
./gradlew :cecamed-api:bootJar
```

`check` ejecuta las verificaciones de los módulos. Las pruebas ordinarias de API utilizan H2 y excluyen la etiqueta `postgres`; no requieren una instancia PostgreSQL. El JAR ejecutable se genera en `cecamed-api/build/libs/cecamed-backend.jar`.

Para validar migraciones, flujos clínicos y agenda contra PostgreSQL, preparar una **base desechable independiente**:

```bash
TEST_DB_URL=jdbc:postgresql://localhost:5433/cecamed_test \
TEST_DB_USERNAME=usuario_de_pruebas \
TEST_DB_PASSWORD=clave_de_pruebas \
./gradlew :cecamed-api:postgresTest
```

La base y el usuario deben existir previamente. Estas pruebas usan `TEST_DB_*`, aplican migraciones y escriben datos; no deben apuntar a la base de trabajo.

### Frontend

Desde `cecamed-frontend/`:

| Comando | Resultado |
| --- | --- |
| `npm run dev` | Desarrollo con API real. |
| `npm run dev:mock` | Desarrollo con simulador. |
| `npm run typecheck` | Comprobación de tipos. |
| `npm test` | Pruebas de renovación, expiración, cierre de sesión y fallos de red. |
| `npm run build` | Comprobación de tipos y archivos de producción en `dist/`. |
| `npm run build:mock` | Compilación de demostración con simulador. |
| `npm run preview` | Vista previa local de la compilación. |
| `npm run test:e2e` | Flujo de citas en navegador contra una API real. |

Para E2E, iniciar previamente API y frontend contra una base desechable y proporcionar una cuenta administradora de esa base:

```bash
npx playwright install chromium
E2E_BASE_URL=http://127.0.0.1:5173 \
E2E_ADMIN_USERNAME=administrador_de_pruebas \
E2E_ADMIN_PASSWORD=clave_de_pruebas \
npm run test:e2e
```

La prueba crea tres médicos, personal de recepción, un paciente y reservas; verifica conflicto de horario, recarga de disponibilidad, confirmación, reprogramación, cancelación e historial. Usa un navegador en UTC para comprobar la conversión horaria. Los datos creados no se eliminan al terminar. Las llamadas de preparación usan `/api/v1` en `E2E_BASE_URL`, por lo que ese origen debe exponer el proxy o la API.

## Almacenamiento de documentos

`DOCUMENTS_DIRECTORY` contiene archivos con nombres UUID generados por el servidor.
El nombre original se conserva únicamente como metadato. Se verifica tamaño, firma
binaria y correspondencia con la extensión; esto no sustituye un análisis antivirus
ni un parser completo de PDF.

Las descargas autenticadas usan `Cache-Control: no-store` y `Content-Disposition: attachment`.
No publicar este directorio como archivos estáticos. Respaldar y restaurar el
directorio junto con PostgreSQL.

Al eliminar, se marca el registro como inactivo y se borra el archivo. Si falla
el borrado en disco, el documento deja de ser accesible y DELETE puede reintentarse.
Se conservan los metadatos. Si falla la persistencia durante una carga, se limpia
el archivo recién creado. Una interrupción abrupta entre operaciones puede dejar
archivos huérfanos; su conciliación no es automática.

## Despliegue

1. Generar el JAR con `./gradlew :cecamed-api:bootJar`.
2. Suministrar `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` al proceso; configurar CORS si se usan orígenes distintos.
3. Ejecutar `java -jar cecamed-api/build/libs/cecamed-backend.jar`.
4. Compilar el frontend con `npm ci` y `npm run build` en su directorio; servir `dist/` desde un servidor web.

Para un mismo origen, el servidor web debe reenviar `/api` a la API y resolver las rutas de la aplicación hacia `index.html`. Para una API en otro origen, compilar con su dirección:

```bash
VITE_API_BASE_URL=https://api.ejemplo.com/api/v1 npm run build
```

El backend no incorpora automáticamente el contenido de `dist/` al JAR. Swagger y OpenAPI están deshabilitados en el perfil `prod`. Readiness permite comprobar que la base está disponible antes de enviar tráfico.

## Problemas frecuentes

| Síntoma | Revisar |
| --- | --- |
| Gradle no inicia | JDK 21, `JAVA_HOME`, permisos del wrapper y descarga inicial de dependencias. |
| La API no conecta a PostgreSQL | `docker compose ps`, `DB_PASSWORD`, puerto `5433` y precedencia de `DB_URL`. |
| Cambiar `.env` no cambia la contraseña de PostgreSQL | El volumen conserva la base inicializada; actualizar la contraseña en PostgreSQL y la configuración de forma coordinada. |
| El navegador no alcanza la API | Destino `CECAMED_API_URL`, proxy `/api` o `VITE_API_BASE_URL`; CORS si hay distintos orígenes. |
| Respuesta 403 | Rol y asociación cuenta-médico o cuenta-paciente; los permisos de ADMIN no incluyen información clínica. |
| Falla Flyway | Revisar historial, checksums y disponibilidad de `btree_gist`; no ejecutar reparaciones automáticas. |
| Documentos fallan con API real | Migración V7 aplicada, rol MEDICO y permisos de lectura/escritura en `DOCUMENTS_DIRECTORY`. |
| Aparecen datos de ejemplo | Verificar modo `mock` y `VITE_USE_MOCK`; recompilar para cambiar el modo de una distribución. |

## Documentación adicional

- [Arquitectura](docs/architecture.md).
- [Contrato de API](docs/api-contract.md).
- [Decisiones del frontend](docs/frontend.md).
- [README del frontend](cecamed-frontend/README.md).
- [Contrato histórico de pantallas](cecamed-frontend/docs/contrato-api-esperado.md).

Algunos documentos conservan referencias a fases anteriores, roles o endpoints antiguos. Para integrar una funcionalidad, contrastar esos textos con los controladores, la configuración de seguridad, los DTO actuales y OpenAPI generado por la aplicación.
