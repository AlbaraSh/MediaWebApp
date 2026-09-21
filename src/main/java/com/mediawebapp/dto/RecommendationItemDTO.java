package com.mediawebapp.dto;

import java.util.UUID;

/**
 * One recommended catalog item. {@code mediaType} is the seeded name
 * ({@code Movie}, {@code TV Show}, {@code Anime}, {@code Game}).
 */
public record RecommendationItemDTO(
		UUID mediaId,
		String title,
		String mediaType,
		Short releaseYear,
		Double externalRating,
		Integer externalRatingCount,
		String posterUrl
) {
	public RecommendationItemDTO(
			UUID mediaId,
			String title,
			String mediaType,
			Short releaseYear,
			Double externalRating,
			Integer externalRatingCount) {
		this(mediaId, title, mediaType, releaseYear, externalRating, externalRatingCount, null);
	}
}
