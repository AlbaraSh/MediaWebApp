import { decodeJwt } from "@/lib/jwt";
import { getStoredUsername } from "@/lib/storage";
import { useAuthStore } from "@/store/authStore";

export function useAuth() {
  const token = useAuthStore((state) => state.token);
  const setToken = useAuthStore((state) => state.setToken);
  const claims = token ? decodeJwt(token) : null;

  return {
    token,
    setToken,
    isAuthenticated: Boolean(token),
    email: claims?.email ?? null,
    userId: claims?.userId ?? null,
    username: getStoredUsername(),
  };
}
