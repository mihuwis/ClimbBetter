import { authMode, requireFirebaseAuth } from "../lib/firebase";
import { assertAuthSession, authSession } from "./auth-session";

export class ApiError extends Error {
  status: number;
  code?: string;
  clientEntryId?: string;
  constructor(status: number, body: Record<string, unknown>) {
    super(
      String(
        body.detail ??
          body.message ??
          body.title ??
          `Request failed (${status})`,
      ),
    );
    this.status = status;
    this.code = typeof body.code === "string" ? body.code : undefined;
    this.clientEntryId =
      typeof body.clientEntryId === "string" ? body.clientEntryId : undefined;
  }
}

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const base = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "");
  const session = authSession();
  const user = authMode === "stub" ? null : requireFirebaseAuth().currentUser;
  if (authMode !== "stub" && !user) throw new ApiError(401, { code: "AUTH_REQUIRED", detail: "Please sign in." });
  const headers = new Headers(options.headers);
  if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
  const signal = options.signal ? AbortSignal.any([options.signal, session.signal]) : session.signal;
  const assertCurrentAccount = () => {
    assertAuthSession(session.generation);
    if (user && requireFirebaseAuth().currentUser?.uid !== user.uid) {
      throw new DOMException("The signed-in account changed.", "AbortError");
    }
  };
  const send = async (refresh: boolean) => {
    // Let a render cleanup cancel requests before they reach the network.
    await Promise.resolve();
    signal.throwIfAborted();
    if (user) headers.set("Authorization", `Bearer ${await user.getIdToken(refresh)}`);
    assertCurrentAccount();
    const response = await fetch(`${base}/api/v1${path}`, { ...options, headers, signal });
    assertCurrentAccount();
    return response;
  };
  let response = await send(false);
  if (response.status === 401 && user) {
    await response.body?.cancel();
    try { response = await send(true); }
    catch (error) {
      assertAuthSession(session.generation);
      const code = typeof error === "object" && error !== null && "code" in error ? String(error.code) : "";
      if (["auth/user-token-expired", "auth/invalid-user-token", "auth/user-disabled"].includes(code)) {
        window.dispatchEvent(new Event("climbbetter:auth-expired"));
      }
      throw error;
    }
    if (response.status === 401) window.dispatchEvent(new Event("climbbetter:auth-expired"));
  }
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    assertCurrentAccount();
    throw new ApiError(response.status, body);
  }
  const result = response.status === 204 ? (undefined as T) : await response.json();
  assertCurrentAccount();
  return result;
}

export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Something went wrong. Please try again.";
}
