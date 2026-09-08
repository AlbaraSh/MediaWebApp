package com.mediawebapp.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Outgoing representation of a media item returned by GET/POST media endpoints.
 * <p>
 * Decouples the public API from the JPA {@code Media} entity so persistence
 * details (lazy associations, column names, internal fields) never leak to
 * clients. {@link MediaTypeDTO} is nested so consumers see a clear media-type
 * object rather than a bare {@code mediaTypeId}.
 */
public record MediaResponseDTO(
		UUID id,
		String title,
		String description,
		Short releaseYear,
		MediaTypeDTO mediaType,
		Instant createdAt,
		Instant updatedAt
) {
}
