package com.mediawebapp.exception;

/** Missing catalog or shelf row. {@link GlobalExceptionHandler} turns this into HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
