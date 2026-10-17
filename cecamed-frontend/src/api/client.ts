import type { ApiErrorBody } from './types';

/**
 * Prefijo de la API. El backend del equipo (cecamed-api) publica sus rutas en /api/v1.
 * En desarrollo Vite reenvía /api al backend (ver vite.config.ts), así que no hace falta CORS.
 * Para apuntar a otro servidor sin proxy: VITE_API_BASE_URL=https://servidor/api/v1
 */
export const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/$/, '') || '/api/v1';

/** Error devuelto por la API (Problem Details). `code` es estable; `message` se muestra al usuario. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string | null;
  readonly body: ApiErrorBody | null;

  constructor(status: number, message: string, body: ApiErrorBody | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = body?.code ?? null;
    this.body = body;
  }
}

/** Evento global que dispara el cliente cuando la sesión expiró (HTTP 401). */
export const UNAUTHORIZED_EVENT = 'cecamed:unauthorized';

// ---------- Token de acceso (Authorization: Bearer) ----------
// El backend es stateless y no usa cookies; el token se guarda solo durante la pestaña.
const TOKEN_KEY = 'cecamed.token';
let accessToken: string | null = null;
try {
  accessToken = sessionStorage.getItem(TOKEN_KEY);
} catch {
  accessToken = null;
}

export function setAccessToken(token: string | null) {
  accessToken = token;
  try {
    if (token) sessionStorage.setItem(TOKEN_KEY, token);
    else sessionStorage.removeItem(TOKEN_KEY);
  } catch {
    // almacenamiento no disponible
  }
}

export function getAccessToken(): string | null {
  return accessToken;
}

type QueryValue = string | number | boolean | null | undefined;

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
  query?: Record<string, QueryValue>;
  /** No emitir UNAUTHORIZED_EVENT ante un 401 (login y comprobación inicial de sesión). */
  silentAuth?: boolean;
  signal?: AbortSignal;
}

export function buildUrl(path: string, query?: Record<string, QueryValue>): string {
  const params = new URLSearchParams();
  if (query) {
    for (const [key, value] of Object.entries(query)) {
      if (value !== undefined && value !== null && value !== '') {
        params.append(key, String(value));
      }
    }
  }
  const qs = params.toString();
  return `${API_BASE_URL}${path}${qs ? `?${qs}` : ''}`;
}

/**
 * Envía la petición y aplica las reglas comunes a todas las llamadas: token Bearer, sesión
 * expirada (401) y errores Problem Details. Devuelve la respuesta solo si fue exitosa.
 */
async function send(path: string, options: RequestOptions, accept: string): Promise<Response> {
  const { method = 'GET', body, query, silentAuth = false, signal } = options;
  const isForm = body instanceof FormData;

  // Solo cabeceras permitidas por el CORS del backend: Authorization, Content-Type y Accept.
  const headers: Record<string, string> = { Accept: accept };
  if (body !== undefined && !isForm) headers['Content-Type'] = 'application/json';
  if (accessToken) headers.Authorization = `Bearer ${accessToken}`;

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : isForm ? (body as FormData) : JSON.stringify(body),
      signal,
    });
  } catch (err) {
    if (err instanceof DOMException && err.name === 'AbortError') throw err;
    throw new ApiError(0, 'No hay conexión con el servidor de CECAMed. Verifique que el backend esté en ejecución.', null);
  }

  if (response.status === 401 && !silentAuth) {
    setAccessToken(null);
    window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
  }

  if (!response.ok) {
    let errorBody: ApiErrorBody | null = null;
    try {
      errorBody = (await response.json()) as ApiErrorBody;
    } catch {
      errorBody = null;
    }
    throw new ApiError(response.status, problemMessage(errorBody, response.status), errorBody);
  }
  return response;
}

/** Petición JSON a la API. */
export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const response = await send(path, options, 'application/json, application/problem+json');
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}

/** Texto para el usuario a partir de Problem Details (detail + errores de validación). */
/**
 * Descarga un archivo binario (por ejemplo, un documento del paciente) con las mismas reglas que
 * `request`: envía el token, cierra la sesión ante un 401 e interpreta los errores Problem Details.
 */
export async function requestBlob(path: string, options: RequestOptions = {}): Promise<Blob> {
  const response = await send(path, options, '*/*');
  return response.blob();
}

function problemMessage(body: ApiErrorBody | null, status: number): string {
  const fieldMessages = (body?.errors ?? []).map((e) => e.message).filter(Boolean);
  if (body?.code === 'VALIDATION_ERROR' && fieldMessages.length > 0) {
    return fieldMessages.join('. ');
  }
  return body?.detail || body?.message || defaultMessage(status);
}

function defaultMessage(status: number): string {
  switch (status) {
    case 401:
      return 'La sesión no está iniciada o ha expirado';
    case 403:
      return 'No tiene permisos para realizar esta operación';
    case 404:
      return 'El recurso solicitado no existe';
    case 413:
      return 'El archivo es demasiado grande';
    default:
      return status >= 500 ? 'Error interno del servidor' : 'No fue posible completar la operación';
  }
}

export function errorMessage(err: unknown, fallback = 'Ocurrió un error inesperado'): string {
  if (err instanceof ApiError) return err.message;
  if (err instanceof Error && err.message) return err.message;
  return fallback;
}
