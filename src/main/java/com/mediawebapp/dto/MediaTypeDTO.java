package com.mediawebapp.dto;

import java.util.UUID;

/**
 * API representation of a media type (e.g. Movie, TV Show).
 * <p>
 * Nested inside {@link MediaResponseDTO} so clients receive a structured
 * media type object instead of a raw foreign-key UUID. This keeps the API
 * contract independent of the {@code media_types} table shape.
 */
public record MediaTypeDTO(
		UUID id,
		String name
) {
}
