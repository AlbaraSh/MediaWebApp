package com.mediawebapp.exception;

/**
 * Thrown when login email/password do not match a stored user.
 * <p>
 * Handled by {@link GlobalExceptionHandler} as HTTP 401. The message is
 * intentionally generic so callers cannot tell whether the email or password
 * was wrong.
 */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException(String message) {
		super(message);
	}
}
