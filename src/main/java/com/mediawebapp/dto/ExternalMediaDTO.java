package com.mediawebapp.dto;

import java.util.List;

/**
 * Unified representation of media from an external provider.
 * {@code genres} are raw provider names; DB Title Case happens at import.
 * Ratings are on a 0–10 scale (RAWG is converted in the mapper).
 */
public record ExternalMediaDTO(
		String title,
		String description,
		Short releaseYear,
		String mediaType,
		String provider,
		String externalId,
		List<String> genres,
		Double externalRating,
		Integer externalRatingCount,
		String posterUrl
) {
	public ExternalMediaDTO(
			String title,
			String description,
			Short releaseYear,
			String mediaType,
			String provider,
			String externalId,
			List<String> genres,
			Double externalRating,
			Integer externalRatingCount) {
		this(title, description, releaseYear, mediaType, provider, externalId, genres,
				externalRating, externalRatingCount, null);
	}
}
