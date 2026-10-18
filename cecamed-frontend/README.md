# CECAMed Frontend

Interfaz web de CECAMed construida con **React 18, TypeScript y Vite**. Reemplaza la interfaz de escritorio JavaFX del CECAMed original y consume la API REST del backend del equipo (`cecamed-api`, rutas `/api/v1`).

## Estado

| Parte | Estado |
| --- | --- |
| Pantallas | Listas: acceso, panel principal, pacientes, expediente clínico (antecedentes, consultas, nueva consulta, citas y documentos), agenda, recepción con sala de espera y configuración. Tema claro/oscuro y diseño adaptable a celular. |
| Conexión con `cecamed-api` | Integrada con `/api/v1`, errores *Problem Details*, Bearer, refresh rotatorio y logout con revocación. |
| Endpoints de negocio | Pacientes, expediente, consultas, médicos, horarios, agenda, recepción e historial integrados con la API real. Adjuntos clínicos implementados: carga, lista, descarga y eliminación de PDF/PNG/JPEG hasta 15 MB, solo MEDICO. Contrato vigente: [api-contract.md](../docs/api-contract.md). |
| API simulada | `npm run dev:mock` permite usar todas las pantallas con datos de ejemplo mientras el backend avanza. |

## Requisitos

- Node.js 20 o superior y npm.
- Para usar la API real: el backend `cecamed-api` en ejecución (por defecto `http://localhost:8080`).

## Ejecutar

```bash
npm ci                 # primera vez o al cambiar el lockfile

npm run dev:mock       # con API simulada y datos de ejemplo  -> http://localhost:5173
npm run dev            # con el backend real en :8080          -> http://localhost:5173
```

Con la API simulada se puede entrar como `medico` / `medico123` o `recepcion` / `recepcion123`. Los datos se reinician al recargar la página.

Con `npm run dev`, Vite reenvía `/api/**` al backend, así que no se necesita configurar CORS. Si el backend corre en otro puerto o equipo:

```bash
# PowerShell
$env:CECAMED_API_URL="http://192.168.1.20:8080"; npm run dev
# Bash
CECAMED_API_URL=http://192.168.1.20:8080 npm run dev
```

| Script | Descripción |
| --- | --- |
| `npm run dev` | Desarrollo contra el backend real. |
| `npm run dev:mock` | Desarrollo con la API simulada. |
| `npm run build` | Comprueba tipos y genera `dist/` para producción (sin la API simulada). |
| `npm run build:mock` | Compilación de demostración con la API simulada. |
| `npm run typecheck` | Solo comprobación de tipos. |
| `npm test` | Rotación de sesión, expiración, logout y errores de red. |
| `npm run preview` | Sirve `dist/` localmente. |

## Producción

`npm run build` genera archivos estáticos en `dist/`. Se pueden servir desde cualquier servidor web (Nginx, el propio Spring Boot, etc.). Si el frontend y la API quedan en dominios distintos:

1. Compilar con `VITE_API_BASE_URL=https://dominio-de-la-api/api/v1`.
2. Agregar el dominio del frontend a `CORS_ALLOWED_ORIGINS` en el backend.

El servidor debe responder `index.html` para cualquier ruta del cliente (`/pacientes`, `/agenda`, …), porque la navegación la resuelve React Router.

## Estructura

```text
src/
├── api/            # Cliente HTTP, endpoints y tipos de los DTO
├── auth/           # Sesión (token) y guardas de ruta por rol
├── components/     # Modal, confirmación, notificaciones, insignias, tarjetas
├── layout/         # Barra superior con navegación, usuario, tema y reloj
├── mocks/          # API simulada (solo modo mock)
├── pages/          # Una página por vista de la aplicación de escritorio
├── theme/          # Tema claro/oscuro
├── utils/          # Fechas, etiquetas en español y hooks
├── App.tsx         # Rutas
├── main.tsx        # Punto de entrada
└── styles.css      # Estilos: colores de la versión de escritorio (rojo médico y celeste) y tipografía Atkinson Hyperlegible
docs/
└── contrato-api-esperado.md   # Endpoints que necesita el frontend
```

La arquitectura y las decisiones del frontend (sesión, fechas en hora de la clínica, carga de datos, colores y pendientes) están en [docs/frontend.md](../docs/frontend.md), en la raíz del repositorio.

## Equivalencias con la versión JavaFX

| JavaFX (CECAMed original) | React | Ruta |
| --- | --- | --- |
| `login.fxml` / `LoginController` | `pages/LoginPage.tsx` | `/login` |
| `main-layout.fxml` / `MainLayoutController` | `layout/MainLayout.tsx` | — |
| `dashboard.fxml` / `DashboardController` | `pages/DashboardPage.tsx` | `/` |
| `patient-list.fxml`, `patient-form-dialog.fxml` | `pages/PatientsPage.tsx`, `pages/PatientFormModal.tsx` | `/pacientes` |
| `medical-record.fxml` / `MedicalRecordController` | `pages/MedicalRecordPage.tsx`, `pages/DocumentsTab.tsx` | `/expediente/:id` |
| `appointment-calendar.fxml`, `appointment-dialog.fxml` | `pages/AppointmentsPage.tsx`, `pages/AppointmentFormModal.tsx` | `/agenda` |
| `reception.fxml` / `ReceptionController` | `pages/ReceptionPage.tsx` | `/recepcion` |
| `settings.fxml` / `SettingsController` | `pages/SettingsPage.tsx` | `/configuracion` |
| `NavigationService`, `UserSession`, `ThemeManager` | React Router, `auth/AuthContext.tsx`, `theme/ThemeContext.tsx` | — |
| `NotificationService`, `ConfirmationDialog` | `components/Toast.tsx`, `components/ConfirmDialog.tsx` | — |

## Convenciones

- **Errores:** el cliente lanza `ApiError` con `status`, `code` y un mensaje tomado de `detail` (o de `errors[].message` en validaciones). Las decisiones se basan en `code`, como pide el contrato del backend.
- **Autenticación:** el token del login se envía como `Authorization: Bearer` y se guarda solo en la pestaña (`sessionStorage`). Ante un 401 protegido se rota el refresh y se reintenta una vez; si la sesión no es válida, se borran ambos tokens y se vuelve al acceso.
- **Fechas:** usar las funciones de `utils/format.ts`. La interfaz muestra y envía la hora de la clínica (`America/Guatemala`) como instantes ISO 8601 con offset, sin importar la zona del equipo. Para "ahora" se usa `clinicNow()`, no `new Date()`.
- **Roles:** el expediente clínico, las consultas y los documentos son solo para el perfil médico. El backend debe validarlo también.
- **Si cambia un DTO del backend**, actualizar `src/api/types.ts` y, si aplica, `src/mocks/mockApi.ts`.

## Validación de sesión y contratos

`npm test` verifica rotación compartida, expiración, logout durante refresh y
fallos de red. `npm run build` valida TypeScript y genera el cliente real;
`npm run build:mock` verifica también el empaquetado del simulador, que conserva
`@ts-nocheck` y no sustituye pruebas contra PostgreSQL. ADMIN tiene acceso a
configuración; pacientes son para MEDICO/RECEPCION y contenido clínico para MEDICO.

El listado de pacientes usa paginación del servidor. Las ediciones conservan la
versión leída y muestran conflictos sin sobreescribir datos recientes. El instante
de consulta se asigna en el servidor; no existe un selector de fecha retroactiva.

## Verificación del flujo real de citas

`npm test` comprueba sesiones y `npm run build` valida TypeScript. La prueba de
navegador utiliza una API real y crea tres médicos, recepción y un paciente propios;
verifica el conflicto concurrente, la recarga de horarios, confirmación, reprogramación,
cancelación y el historial. Ejecútela únicamente con una base desechable.

```bash
npx playwright install chromium
# Inicie previamente backend y Vite contra la base de pruebas.
E2E_BASE_URL=http://127.0.0.1:5173 \
E2E_ADMIN_USERNAME=usuario_de_prueba \
E2E_ADMIN_PASSWORD=contraseña_de_prueba \
npm run test:e2e
```

Las credenciales deben corresponder al administrador de esa base de pruebas.
El navegador usa UTC para comprobar la conversión a la hora de Guatemala.
