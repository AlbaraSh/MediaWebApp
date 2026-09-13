package com.mediawebapp.exception;

/**
 * Thrown when registration collides with an existing unique value (email or username).
 * <p>
 * Handled by {@link GlobalExceptionHandler} as HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}
}
