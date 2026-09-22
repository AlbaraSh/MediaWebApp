package com.mediawebapp.dto;

import com.mediawebapp.exception.BadRequestException;
import java.util.Locale;

/**
 * Allowed {@code sort} values for GET {@code /api/user-media}.
 * Default is {@code ADDED} (created_at) so omitted params keep the original order.
 */
public enum LibrarySort {
	ADDED,
	RATING;

	public static LibrarySort fromParam(String raw) {
		if (raw == null || raw.isBlank()) {
			return ADDED;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("Invalid sort. Must be one of: ADDED, RATING");
		}
	}
}
