import { apiFetch } from "@/lib/api/client";
import type { AuthResponseDTO, LoginRequestDTO, RegisterRequestDTO } from "@/lib/api/types";

export function registerAccount(body: RegisterRequestDTO): Promise<void> {
  return apiFetch<void>("/api/auth/register", {
    method: "POST",
    json: body,
    errorToast: false,
  });
}

export function loginAccount(body: LoginRequestDTO): Promise<AuthResponseDTO> {
  return apiFetch<AuthResponseDTO>("/api/auth/login", {
    method: "POST",
    json: body,
    errorToast: false,
  });
}

export function logoutAccount(): Promise<void> {
  return apiFetch<void>("/api/auth/logout", {
    method: "POST",
    errorToast: false,
    skipAuthRedirect: true,
  });
}
