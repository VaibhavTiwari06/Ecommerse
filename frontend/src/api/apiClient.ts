// TASK-FE-001 | REQ-FE-001
// Thin fetch wrapper — reads token from localStorage, attaches Authorization header.

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8081';

const TOKEN_KEY = 'ebookstore_token';

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}

export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
}

function authHeaders(): Record<string, string> {
  const token = getToken();
  return token ? { Authorization: `Bearer ${token}` } : {};
}

function toQueryString(params?: Record<string, unknown>): string {
  if (!params) return '';
  const qs = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null && v !== '') {
      qs.set(k, String(v));
    }
  }
  const s = qs.toString();
  return s ? `?${s}` : '';
}

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      message = body.message ?? body.error ?? message;
    } catch {
      // ignore parse error — keep generic message
    }
    throw new Error(message);
  }
  // 204 No Content
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

const apiClient = {
  get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
    return fetch(`${BASE_URL}${url}${toQueryString(params)}`, {
      headers: { ...authHeaders() },
    }).then((r) => handleResponse<T>(r));
  },

  post<T>(url: string, body?: unknown): Promise<T> {
    return fetch(`${BASE_URL}${url}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }).then((r) => handleResponse<T>(r));
  },

  put<T>(url: string, body?: unknown): Promise<T> {
    return fetch(`${BASE_URL}${url}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json', ...authHeaders() },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    }).then((r) => handleResponse<T>(r));
  },

  delete<T>(url: string): Promise<T> {
    return fetch(`${BASE_URL}${url}`, {
      method: 'DELETE',
      headers: { ...authHeaders() },
    }).then((r) => handleResponse<T>(r));
  },
};

export default apiClient;
