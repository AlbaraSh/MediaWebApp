package com.mediawebapp.exception;

import java.util.Map;

/** Client-facing error body. {@code details} is set for field validation; stack traces are never included. */
public record ApiErrorResponse(
		String error,
		int status,
		Map<String, String> details
) {

	public static ApiErrorResponse of(String error, int status) {
		return new ApiErrorResponse(error, status, null);
	}

	public static ApiErrorResponse of(String error, int status, Map<String, String> details) {
		return new ApiErrorResponse(error, status, details);
	}
}
