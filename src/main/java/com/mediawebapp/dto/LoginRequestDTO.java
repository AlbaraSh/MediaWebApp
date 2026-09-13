package com.mediawebapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Incoming payload for {@code POST /api/auth/login}.
 */
public record LoginRequestDTO(

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be a valid email address")
		String email,

		@NotBlank(message = "Password is required")
		String password
) {
}
