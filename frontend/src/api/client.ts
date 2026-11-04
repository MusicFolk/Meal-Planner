/** REST API klientas (NFR-06). Būseną keičiančioms užklausoms siunčiamas CSRF žetonas (NFR-51). */

export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

export const UNAUTHORIZED_EVENT = 'mealplanner:unauthorized';

function getCookie(name: string): string | undefined {
  return document.cookie
    .split('; ')
    .find((c) => c.startsWith(name + '='))
    ?.split('=')[1];
}

async function ensureCsrfToken(force = false): Promise<string> {
  if (force || !getCookie('XSRF-TOKEN')) {
    await fetch('/api/auth/csrf', { credentials: 'same-origin' });
  }
  return decodeURIComponent(getCookie('XSRF-TOKEN') ?? '');
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE';

async function send(path: string, method: Method, body: unknown, forceCsrf: boolean): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = await ensureCsrfToken(forceCsrf);
  return fetch(path, {
    method,
    headers,
    credentials: 'same-origin',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  });
}

export async function request<T>(path: string, method: Method = 'GET', body?: unknown): Promise<T> {
  let response = await send(path, method, body, false);
  // Pasenęs CSRF žetonas (pvz., po atsijungimo) – gaunamas naujas ir bandoma dar kartą
  if (response.status === 403 && method !== 'GET') {
    response = await send(path, method, body, true);
  }
  if (response.status === 204) return undefined as T;
  if (!response.ok) {
    let data: { message?: string; fieldErrors?: Record<string, string> } = {};
    try {
      data = await response.json();
    } catch {
      // atsakymas be turinio
    }
    if (response.status === 401 && !path.startsWith('/api/auth/')) {
      window.dispatchEvent(new Event(UNAUTHORIZED_EVENT));
    }
    throw new ApiError(
      response.status,
      data.message ?? 'Įvyko klaida. Bandykite vėliau.',
      data.fieldErrors ?? {},
    );
  }
  return (await response.json()) as T;
}

export const api = {
  get: <T>(path: string) => request<T>(path, 'GET'),
  post: <T>(path: string, body?: unknown) => request<T>(path, 'POST', body ?? {}),
  put: <T>(path: string, body?: unknown) => request<T>(path, 'PUT', body ?? {}),
  delete: <T>(path: string) => request<T>(path, 'DELETE'),
};

/** Atsisiunčia failą (pvz., duomenų eksportą JSON formatu). */
export async function download(path: string, fallbackName: string): Promise<void> {
  const response = await fetch(path, { credentials: 'same-origin' });
  if (!response.ok) throw new ApiError(response.status, 'Nepavyko atsisiųsti duomenų.');
  const disposition = response.headers.get('Content-Disposition') ?? '';
  const name = /filename="?([^"]+)"?/.exec(disposition)?.[1] ?? fallbackName;
  const url = URL.createObjectURL(await response.blob());
  const link = document.createElement('a');
  link.href = url;
  link.download = name;
  link.click();
  URL.revokeObjectURL(url);
}
