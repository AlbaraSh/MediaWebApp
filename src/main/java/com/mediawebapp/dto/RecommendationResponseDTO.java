package com.mediawebapp.dto;

import java.util.List;

/**
 * Recommendations grouped by seeded media type. Arrays may be empty.
 */
public record RecommendationResponseDTO(
		List<RecommendationItemDTO> movies,
		List<RecommendationItemDTO> tvShows,
		List<RecommendationItemDTO> anime,
		List<RecommendationItemDTO> games
) {

	public static RecommendationResponseDTO empty() {
		return new RecommendationResponseDTO(List.of(), List.of(), List.of(), List.of());
	}
}
