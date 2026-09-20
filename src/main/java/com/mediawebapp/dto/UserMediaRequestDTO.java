package com.mediawebapp.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * Incoming payload for creating or updating a personal media list entry
 * via {@code POST /api/user-media}.
 */
public record UserMediaRequestDTO(

		@NotNull(message = "Media id is required")
		UUID mediaId,

		@NotBlank(message = "Status is required")
		@Pattern(
				regexp = "PLANNED|WATCHING|COMPLETED|DROPPED",
				message = "Status must be one of: PLANNED, WATCHING, COMPLETED, DROPPED"
		)
		String status,

		@Min(value = 1, message = "Rating must be at least 1")
		@Max(value = 10, message = "Rating must be at most 10")
		Integer rating,

		@Size(max = 2000, message = "Review must be at most 2000 characters")
		String review
) {
}
