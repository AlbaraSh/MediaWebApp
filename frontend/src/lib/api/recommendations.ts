import { apiFetch } from "@/lib/api/client";
import type { CatalogType, RecommendationResponseDTO } from "@/lib/api/types";

export function fetchSimilar(
  mediaId: string,
): Promise<RecommendationResponseDTO | null> {
  return apiFetch<RecommendationResponseDTO | null>(
    `/api/recommendations/similar/${mediaId}`,
    { expectedNotFound: "null" },
  );
}

export function fetchForYou(type: CatalogType): Promise<RecommendationResponseDTO> {
  return apiFetch<RecommendationResponseDTO>("/api/recommendations/user", {
    query: { type },
  });
}
