import { ApiError, NotFoundError } from "@/lib/api/errors";
import type { ApiErrorBody } from "@/lib/api/types";
import { currentLocationPath, loginPathWithNext } from "@/lib/redirect";
import { toast } from "@/lib/toast";
import { useAuthStore } from "@/store/authStore";

export type ExpectedNotFound = "throw" | "null";

export type ApiFetchOptions = Omit<RequestInit, "body"> & {
  json?: unknown;
  query?: Record<string, string | number | boolean | undefined | null>;
  expectedNotFound?: ExpectedNotFound;
  errorToast?: boolean;
  skipAuthRedirect?: boolean;
};

type AuthRedirectHandler = (loginPath: string) => void;

let onSessionExpired: AuthRedirectHandler | null = null;
let onUnauthorized: AuthRedirectHandler | null = null;

export function bindAuthRedirects(handlers: {
  onSessionExpired: AuthRedirectHandler;
  onUnauthorized: AuthRedirectHandler;
}): void {
  onSessionExpired = handlers.onSessionExpired;
  onUnauthorized = handlers.onUnauthorized;
}

function apiBase(): string {
  return (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");
}

function isCredentialAuthPath(path: string): boolean {
  return path.startsWith("/api/auth/login") || path.startsWith("/api/auth/register");
}

function toQueryString(
  query: Record<string, string | number | boolean | undefined | null> | undefined,
): string {
  if (!query) {
    return "";
  }
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value === undefined || value === null || value === "") {
      continue;
    }
    params.set(key, String(value));
  }
  const encoded = params.toString();
  return encoded ? `?${encoded}` : "";
}

function parseErrorBody(text: string, status: number): ApiErrorBody {
  if (!text) {
    return { error: `Request failed (${status})`, status, details: null };
  }
  try {
    const parsed = JSON.parse(text) as Partial<ApiErrorBody>;
    return {
      error: parsed.error ?? `Request failed (${status})`,
      status: parsed.status ?? status,
      details: parsed.details ?? null,
    };
  } catch {
    return { error: `Request failed (${status})`, status, details: null };
  }
}

function toastApiError(body: ApiErrorBody): void {
  const extras = body.details ? Object.values(body.details).filter(Boolean) : [];
  const message = extras.length > 0 ? `${body.error}: ${extras.join(" · ")}` : body.error;
  toast(message);
}

function handleUnauthorized(body: ApiErrorBody, skipAuthRedirect: boolean): void {
  if (skipAuthRedirect) {
    return;
  }
  const next = currentLocationPath();
  const loginPath = loginPathWithNext(next);
  const onLoginOrRegister =
    window.location.pathname === "/login" || window.location.pathname === "/register";

  if (body.error === "Invalid or expired JWT") {
    useAuthStore.getState().setToken(null);
    toast("Session expired");
    if (!onLoginOrRegister) {
      onSessionExpired?.(loginPath);
    }
    return;
  }

  if (body.error === "Unauthorized") {
    if (!onLoginOrRegister) {
      onUnauthorized?.(loginPath);
    }
  }
}

async function parseBody<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  if (!text) {
    return undefined as T;
  }
  return JSON.parse(text) as T;
}

export async function apiFetch<T>(path: string, options: ApiFetchOptions = {}): Promise<T> {
  const {
    json,
    query,
    expectedNotFound,
    errorToast = true,
    skipAuthRedirect = false,
    headers: initHeaders,
    ...init
  } = options;

  const token = useAuthStore.getState().token;
  const headers = new Headers(initHeaders);
  if (json !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${apiBase()}${path}${toQueryString(query)}`, {
    ...init,
    headers,
    body: json !== undefined ? JSON.stringify(json) : undefined,
  });

  if (response.ok) {
    return parseBody<T>(response);
  }

  const text = await response.text();
  const body = parseErrorBody(text, response.status);

  if (response.status === 401) {
    if (!isCredentialAuthPath(path)) {
      handleUnauthorized(body, skipAuthRedirect);
    }
    throw new ApiError(body);
  }

  if (response.status === 404) {
    if (expectedNotFound === "null") {
      return null as T;
    }
    if (expectedNotFound === "throw") {
      throw new NotFoundError(body);
    }
    toast(body.error);
    throw new ApiError(body);
  }

  if (response.status === 403) {
    toast("Access denied");
    throw new ApiError(body);
  }

  if (response.status === 503) {
    toast(body.error);
    throw new ApiError(body);
  }

  if (response.status === 429) {
    toast(body.error);
    throw new ApiError(body);
  }

  if (response.status === 400 && errorToast) {
    toastApiError(body);
  }

  throw new ApiError(body);
}
