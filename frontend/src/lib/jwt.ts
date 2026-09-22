export type JwtClaims = {
  userId: string;
  email: string;
  exp?: number;
};

export function decodeJwt(token: string): JwtClaims | null {
  try {
    const segment = token.split(".")[1];
    if (!segment) {
      return null;
    }
    const normalized = segment.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized + "=".repeat((4 - (normalized.length % 4)) % 4);
    const json = globalThis.atob(padded);
    const payload = JSON.parse(json) as {
      userId?: string;
      email?: string;
      exp?: number;
    };
    if (!payload.userId || !payload.email) {
      return null;
    }
    return {
      userId: payload.userId,
      email: payload.email,
      exp: payload.exp,
    };
  } catch {
    return null;
  }
}
