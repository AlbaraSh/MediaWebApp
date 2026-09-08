package com.mediawebapp.exception;

import java.util.Map;

/**
 * Stable JSON error body returned by the API for client-facing failures.
 * <p>
 * Keeps error responses consistent across not-found, validation, and other
 * handled cases. Stack traces are never included — only safe messages.
 *
 * @param error   short summary of the failure
 * @param status  HTTP status code mirrored in the body for client convenience
 * @param details optional field-level messages (used for validation errors);
 *                may be {@code null} when not applicable
 */
public record ApiErrorResponse(
		String error,
		int status,
		Map<String, String> details
) {

	/**
	 * Builds an error response without field-level details.
	 *
	 * @param error  short summary
	 * @param status HTTP status code
	 * @return error body with {@code details} set to {@code null}
	 */
	public static ApiErrorResponse of(String error, int status) {
		return new ApiErrorResponse(error, status, null);
	}

	/**
	 * Builds an error response that includes per-field validation messages.
	 *
	 * @param error   short summary
	 * @param status  HTTP status code
	 * @param details map of field name → message
	 * @return error body including validation details
	 */
	public static ApiErrorResponse of(String error, int status, Map<String, String> details) {
		return new ApiErrorResponse(error, status, details);
	}
}
