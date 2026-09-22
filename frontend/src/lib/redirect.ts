/**
 * `next` must be a same-origin relative path starting with `/`.
 * React Router already decodes the query param once; only decode again when
 * the value is still percent-encoded (does not yet start with `/`).
 */
export function resolvePostAuthPath(next: string | null | undefined): string {
  if (!next) {
    return "/";
  }
  let decoded = next;
  if (!decoded.startsWith("/")) {
    try {
      decoded = decodeURIComponent(next);
    } catch {
      return "/";
    }
  }
  if (!decoded.startsWith("/") || decoded.startsWith("//") || decoded.includes("://")) {
    return "/";
  }
  if (decoded === "/login" || decoded.startsWith("/login?")
      || decoded === "/register" || decoded.startsWith("/register?")) {
    return "/";
  }
  return decoded;
}

export function currentLocationPath(): string {
  return `${window.location.pathname}${window.location.search}`;
}

export function loginPathWithNext(nextPath: string): string {
  return `/login?next=${encodeURIComponent(nextPath)}`;
}
