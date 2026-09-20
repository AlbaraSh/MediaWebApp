package com.mediawebapp.dto;

import com.mediawebapp.entity.UserMediaStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API representation of one entry on a user's personal media list, including a
 * compact summary of the referenced catalog media (never the full Media entity).
 */
public record UserMediaResponseDTO(
		UserMediaStatus status,
		Integer rating,
		String review,
		Instant createdAt,
		Instant updatedAt,
		UUID mediaId,
		String title,
		Short releaseYear,
		MediaTypeDTO mediaType,
		List<String> genres
) {
}
