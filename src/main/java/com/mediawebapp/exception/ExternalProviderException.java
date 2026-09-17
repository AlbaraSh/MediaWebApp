package com.mediawebapp.exception;

/**
 * Thrown when an external media provider fails (timeout, 5xx, unexpected 4xx).
 * {@code retryable} distinguishes transient failures (timeout, 429, 5xx) from
 * definitive client errors other than 404.
 */
public class ExternalProviderException extends RuntimeException {

	private final boolean retryable;

	public ExternalProviderException(String message) {
		this(message, true);
	}

	public ExternalProviderException(String message, boolean retryable) {
		super(message);
		this.retryable = retryable;
	}

	public boolean isRetryable() {
		return retryable;
	}
}
