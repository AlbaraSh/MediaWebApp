const TOKEN_KEY = "mediawebapp.jwt";
const USERNAME_KEY = "mediawebapp.username";

function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function persistToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  } catch {
    // Private mode / blocked storage — memory still holds the token.
  }
}

export function getStoredUsername(): string | null {
  try {
    return localStorage.getItem(USERNAME_KEY);
  } catch {
    return null;
  }
}

export function persistUsername(username: string | null): void {
  try {
    if (username) {
      localStorage.setItem(USERNAME_KEY, username);
    } else {
      localStorage.removeItem(USERNAME_KEY);
    }
  } catch {
    // ignore
  }
}

export { readToken, TOKEN_KEY, USERNAME_KEY };
