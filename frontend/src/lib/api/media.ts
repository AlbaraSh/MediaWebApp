import { apiFetch } from "@/lib/api/client";
import type {
  DiscoverQuery,
  ExternalMediaDTO,
  ImportMediaRequestDTO,
  MediaResponseDTO,
  PageResponse,
} from "@/lib/api/types";

export function fetchDiscover(
  query: DiscoverQuery,
): Promise<PageResponse<MediaResponseDTO>> {
  return apiFetch<PageResponse<MediaResponseDTO>>("/api/media", {
    query: {
      type: query.type,
      genre: query.genre,
      year: query.year,
      q: query.q,
      sort: query.sort,
      direction: query.direction,
      page: query.page,
      size: query.size ?? 20,
    },
  });
}

export function fetchMediaById(id: string): Promise<MediaResponseDTO> {
  return apiFetch<MediaResponseDTO>(`/api/media/${id}`, {
    expectedNotFound: "throw",
  });
}

export function searchExternal(query: string, type: string): Promise<ExternalMediaDTO[]> {
  return apiFetch<ExternalMediaDTO[]>("/api/media/search", {
    query: { query, type },
  });
}

export function importMedia(body: ImportMediaRequestDTO): Promise<MediaResponseDTO> {
  return apiFetch<MediaResponseDTO>("/api/media/import", {
    method: "POST",
    json: body,
  });
}
