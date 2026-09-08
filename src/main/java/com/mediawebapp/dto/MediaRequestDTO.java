package com.mediawebapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Incoming payload for creating a media item via {@code POST /api/media}.
 * <p>
 * Carries only what the client may supply. Database-owned fields
 * ({@code id}, timestamps) are intentionally omitted. Bean Validation
 * annotations enforce rules that mirror the PostgreSQL constraints where
 * practical, so invalid requests fail fast with {@code 400} before any
 * persistence attempt.
 */
public record MediaRequestDTO(

		@NotBlank(message = "Title is required")
		@Size(max = 500, message = "Title must be at most 500 characters")
		String title,

		String description,

		@Min(value = 1800, message = "Release year must be at least 1800")
		@Max(value = 2100, message = "Release year must be at most 2100")
		Short releaseYear,

		@NotNull(message = "Media type id is required")
		UUID mediaTypeId
) {
}
