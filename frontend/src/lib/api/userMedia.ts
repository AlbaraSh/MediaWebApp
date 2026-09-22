import { apiFetch } from "@/lib/api/client";
import type {
  LibraryPageResponse,
  LibraryQuery,
  UserMediaRequestDTO,
  UserMediaResponseDTO,
} from "@/lib/api/types";

export function fetchLibrary(query: LibraryQuery): Promise<LibraryPageResponse> {
  return apiFetch<LibraryPageResponse>("/api/user-media", {
    query: {
      status: query.status,
      type: query.type,
      genre: query.genre,
      minRating: query.minRating,
      maxRating: query.maxRating,
      q: query.q,
      sort: query.sort,
      direction: query.direction,
      page: query.page,
      size: query.size ?? 20,
    },
  });
}

export function fetchShelfEntry(mediaId: string): Promise<UserMediaResponseDTO | null> {
  return apiFetch<UserMediaResponseDTO | null>(`/api/user-media/${mediaId}`, {
    expectedNotFound: "null",
  });
}

export function upsertShelf(body: UserMediaRequestDTO): Promise<UserMediaResponseDTO> {
  return apiFetch<UserMediaResponseDTO>("/api/user-media", {
    method: "POST",
    json: body,
    errorToast: false,
  });
}

export function fetchShelfGenres(): Promise<string[]> {
  return apiFetch<string[]>("/api/user-media/genres");
}

export function deleteShelfEntry(mediaId: string): Promise<void> {
  return apiFetch<void>(`/api/user-media/${mediaId}`, {
    method: "DELETE",
  });
}
