export class ApiError extends Error {
  status: number;
  code?: string;
  clientEntryId?: string;
  constructor(status: number, body: Record<string, unknown>) {
    super(String(body.detail ?? body.message ?? body.title ?? `Request failed (${status})`));
    this.status = status;
    this.code = typeof body.code === 'string' ? body.code : undefined;
    this.clientEntryId = typeof body.clientEntryId === 'string' ? body.clientEntryId : undefined;
  }
}

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const base = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '');
  const response = await fetch(`${base}/api/v1${path}`, {
    ...options,
    headers: { ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    throw new ApiError(response.status, body);
  }
  return response.status === 204 ? undefined as T : response.json();
}

export function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'Something went wrong. Please try again.';
}
