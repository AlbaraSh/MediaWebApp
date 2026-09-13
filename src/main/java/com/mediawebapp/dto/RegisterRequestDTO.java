package com.mediawebapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Incoming payload for {@code POST /api/auth/register}.
 * <p>
 * {@code username} is a unique display name only — login uses email + password.
 */
public record RegisterRequestDTO(

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be a valid email address")
		@Size(max = 255, message = "Email must be at most 255 characters")
		String email,

		@NotBlank(message = "Username is required")
		@Size(max = 50, message = "Username must be at most 50 characters")
		String username,

		@NotBlank(message = "Password must not be blank")
		@Size(min = 8, message = "Password must be at least 8 characters")
		String password
) {
}
