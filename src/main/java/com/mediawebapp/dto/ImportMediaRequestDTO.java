package com.mediawebapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Incoming payload for {@code POST /api/media/import}.
 * <p>
 * {@code provider} is lowercase only; the service converts it to UPPERCASE
 * before reading or writing {@code media_external_ids.source}.
 */
public record ImportMediaRequestDTO(

		@NotBlank(message = "Provider is required")
		@Pattern(
				regexp = "tmdb|jikan|rawg",
				message = "Provider must be one of: tmdb, jikan, rawg"
		)
		String provider,

		@NotBlank(message = "External id is required")
		String externalId
) {
}
