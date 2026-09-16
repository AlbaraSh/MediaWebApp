package com.mediawebapp.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Outgoing representation of a media item returned by GET/POST media endpoints.
 * Genres are derived from {@code media_genres} / {@code genres}, not a media column.
 */
public record MediaResponseDTO(
		UUID id,
		String title,
		String description,
		Short releaseYear,
		List<String> genres,
		Double rating,
		Integer ratingCount,
		MediaTypeDTO mediaType,
		Instant createdAt,
		Instant updatedAt
) {
}
