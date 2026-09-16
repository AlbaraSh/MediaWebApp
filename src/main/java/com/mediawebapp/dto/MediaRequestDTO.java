package com.mediawebapp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Incoming payload for creating a media item via {@code POST /api/media}.
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
		UUID mediaTypeId,

		List<String> genres,

		@DecimalMin(value = "0.0", message = "Rating must be at least 0")
		@DecimalMax(value = "10.0", message = "Rating must be at most 10")
		Double rating,

		@Min(value = 0, message = "Rating count must be at least 0")
		Integer ratingCount
) {
}
