import { apiFetch } from "@/lib/api/client";

export function fetchGenres(): Promise<string[]> {
  return apiFetch<string[]>("/api/genres");
}
