package com.mediawebapp.dto;

/**
 * Library switcher totals. Counts apply type/genre/q filters but ignore status,
 * rating bounds, and pagination.
 */
public record UserMediaStatusCounts(
		long planned,
		long watching,
		long completed,
		long dropped
) {

	public static UserMediaStatusCounts empty() {
		return new UserMediaStatusCounts(0, 0, 0, 0);
	}
}
