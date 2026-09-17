package com.mediawebapp.exception;

import java.util.Map;

/**
 * Thrown for client-input errors that are not Bean Validation failures
 * (invalid provider/type, missing release year on import, malformed TMDB ids).
 * <p>
 * Handled by {@link GlobalExceptionHandler} as HTTP 400.
 */
public class BadRequestException extends RuntimeException {

	private final Map<String, String> details;

	public BadRequestException(String message) {
		this(message, null);
	}

	public BadRequestException(String message, Map<String, String> details) {
		super(message);
		this.details = details;
	}

	public Map<String, String> getDetails() {
		return details;
	}
}
