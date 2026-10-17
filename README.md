# CECAMed Backend

Backend independiente para migrar CECAMed de escritorio a una aplicación web.
Esta entrega corresponde únicamente a la arquitectura inicial; las funcionalidades
clínicas se incorporarán por etapas. No tiene dependencias de JavaFX.

El frontend web está en `cecamed-frontend/` (React, TypeScript y Vite) y consume
esta API. Ver [Frontend](#frontend).

## Stack

- Java 21.
- Spring Boot 4.1.1 y Spring MVC.
- Gradle 9.7.1 con Kotlin DSL y wrapper incluido.
- Spring Data JPA, PostgreSQL 16 y Flyway.
- Spring Security, Bean Validation, Actuator y OpenAPI con springdoc 3.1.1.
- JUnit y MockMvc; H2 solo para comprobar el contexto de pruebas.

Se utiliza Spring Boot 4 para una base nueva, independiente del Spring Boot 3.3.4
del escritorio. La compatibilidad de Java y Gradle se puede consultar en los
[requisitos oficiales](https://docs.spring.io/spring-boot/system-requirements.html).
La integración OpenAPI sigue la [documentación de springdoc](https://springdoc.org/).

## Arquitectura

Monolito modular con puertos y adaptadores, desplegado como un único JAR.
Los módulos Gradle establecen las dependencias entre capas; dentro de cada módulo,
los paquetes se organizarán por funcionalidad al incorporarla.

```text
CECAMed-backend/
├── cecamed-domain/           # Modelo y reglas de negocio, Java puro
├── cecamed-application/      # Casos de uso y contratos de puertos
├── cecamed-infrastructure/   # JPA, migraciones, archivos e integraciones
├── cecamed-api/              # Spring Boot, REST, seguridad y contrato OpenAPI
├── cecamed-frontend/         # Frontend web: React, TypeScript y Vite
├── docs/
│   ├── architecture.md      # Límites, decisiones y plan de migración
│   ├── api-contract.md      # Guía de integración para el frontend
│   └── frontend.md          # Arquitectura y decisiones del frontend
├── gradle/wrapper/
├── compose.yaml             # PostgreSQL local independiente
└── .env.example
```

```mermaid
flowchart LR
    Frontend[cecamed-frontend] -->|HTTP / JSON| API[cecamed-api]
    API --> Application[cecamed-application]
    API --> Infrastructure[cecamed-infrastructure]
    Infrastructure --> Application
    Application --> Domain[cecamed-domain]
    Infrastructure --> PostgreSQL[(PostgreSQL)]
```

La API incorpora infraestructura para componer la aplicación. Los controladores
de negocio utilizarán los puertos de entrada de aplicación, sin llamar repositorios.
Las reglas y el proceso de incorporación de funcionalidades están en
[architecture.md](docs/architecture.md).

## Arranque local

Requiere JDK 21, Docker Compose y conexión a Internet para la primera descarga.
No es necesario instalar Gradle. Ejecutar desde la raíz de este proyecto:

```bash
cp .env.example .env
# Editar DB_PASSWORD antes de iniciar PostgreSQL.
docker compose up -d --wait postgres
./gradlew :cecamed-api:bootRun
```

El perfil predeterminado es `local`. Spring importa `.env` como propiedades desde
la raíz. Las variables exportadas en el entorno tienen prioridad. En el IDE,
seleccionar JDK 21 y usar la raíz de este proyecto como directorio de trabajo.
En Windows, utilizar `gradlew.bat`.

La API escucha en `http://127.0.0.1:8080` y PostgreSQL publica el puerto `5433`.
Para conectar desde otro equipo, configurar `SERVER_ADDRESS` y el origen CORS.
Si se cambia `DB_PORT`, actualizar también el puerto en `DB_URL`.

| Recurso | Dirección local |
| --- | --- |
| Información de versión | `GET /api/v1/system/info` |
| Estado del servicio | `GET /actuator/health` |
| Disponibilidad con base de datos | `GET /actuator/health/readiness` |
| Estado del proceso | `GET /actuator/health/liveness` |
| Contrato OpenAPI JSON | `GET /v3/api-docs` |
| Documentación interactiva | `/swagger-ui/index.html` |

La documentación interactiva está incluida en el backend para explorar la API.
Los contratos y ejemplos de integración están en [api-contract.md](docs/api-contract.md).

```bash
curl http://localhost:8080/api/v1/system/info
curl http://localhost:8080/actuator/health
```

## Frontend

La interfaz web reemplaza las pantallas JavaFX y vive en `cecamed-frontend/`. Se
desarrolla con Node.js 20 o superior, independiente de Gradle:

```bash
cd cecamed-frontend
npm ci
npm run dev        # contra esta API en :8080, en http://localhost:5173
npm run dev:mock   # con API simulada, sin backend
```

En desarrollo, Vite reenvía `/api` al backend, así que no hace falta configurar CORS.
Mientras no existan los endpoints de negocio, `npm run dev:mock` permite usar todas las
pantallas con datos de ejemplo (`medico` / `medico123`, `recepcion` / `recepcion123`).

| Documento | Contenido |
| --- | --- |
| [cecamed-frontend/README.md](cecamed-frontend/README.md) | Ejecución, scripts y estructura |
| [docs/frontend.md](docs/frontend.md) | Arquitectura, sesión, fechas, apariencia y pendientes |
| [contrato-api-esperado.md](cecamed-frontend/docs/contrato-api-esperado.md) | Rutas y DTO que el frontend necesita de esta API |

## Configuración

| Variable | Uso |
| --- | --- |
| `DB_URL` | URL JDBC; el ejemplo apunta a la base nueva `cecamed_backend`. |
| `DB_NAME` | Base que crea Compose. Debe coincidir con la base de `DB_URL`. |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales de PostgreSQL. |
| `DB_PORT` | Puerto local que publica Compose; predeterminado `5433`. |
| `SERVER_PORT` | Puerto HTTP; predeterminado `8080`. |
| `SERVER_ADDRESS` | Dirección de escucha; en local `127.0.0.1`. |
| `CORS_ALLOWED_ORIGINS` | Orígenes del frontend separados por comas, sin barra final. |
| `SPRING_PROFILES_ACTIVE` | `local` o `prod`; predeterminado `local`. |

El perfil `prod` exige URL y usuario explícitos, desactiva OpenAPI/Swagger y no
autoriza orígenes CORS salvo los configurados. La autenticación web todavía no está
implementada: Spring Security bloquea todas las rutas salvo los recursos técnicos
indicados. No existen usuarios ni contraseñas de aplicación predefinidos.

## Compilación y comprobaciones

```bash
./gradlew build
./gradlew :cecamed-api:test
java -jar cecamed-api/build/libs/cecamed-backend.jar
```

Las pruebas cubren arranque de contexto, contrato OpenAPI, errores HTTP,
validación de entradas, bloqueo de rutas y CORS. Usan H2 con Flyway desactivado;
no certifican las migraciones ni el comportamiento específico de PostgreSQL.
Al incorporar persistencia de negocio se añadirán pruebas contra PostgreSQL.
Los informes se generan en `cecamed-api/build/reports/tests/test/index.html`.

## Base de datos y alcance de esta etapa

Se mantienen las tecnologías de persistencia del CECAMed original, con una base
y un volumen nuevos. Flyway es el único responsable de evolucionar el esquema;
Hibernate utiliza `ddl-auto=validate`. Aún no existen tablas de negocio ni
migraciones SQL versionadas. Flyway preparará su esquema e historial al arrancar.

La entrega incluye módulos, configuración, manejo uniforme de errores, CORS,
seguridad restrictiva, OpenAPI, información de versión y salud del servicio.
Quedan para siguientes etapas la autenticación y roles, pacientes, expedientes,
consultas, citas, recepción, documentos, inventario y Google Calendar.
El código y los datos de `../CECAMed` no se modifican ni se copian automáticamente.

Para detener la base conservando sus datos:

```bash
docker compose down
```
