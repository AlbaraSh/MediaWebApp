package com.mediawebapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** {@code provider} is lowercase. It is stored uppercase on {@code media_external_ids.source}. */
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
