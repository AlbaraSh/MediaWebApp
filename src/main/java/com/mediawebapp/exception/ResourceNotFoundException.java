package com.mediawebapp.exception;

/**
 * Thrown when a requested domain resource does not exist in the database.
 * <p>
 * Handled by {@link com.mediawebapp.exception.GlobalExceptionHandler} and
 * translated into an HTTP {@code 404} JSON body. Controllers never catch this
 * themselves — the service layer raises it when a lookup fails.
 */
public class ResourceNotFoundException extends RuntimeException {

	/**
	 * @param message human-readable description of what was missing
	 */
	public ResourceNotFoundException(String message) {
		super(message);
	}
}
