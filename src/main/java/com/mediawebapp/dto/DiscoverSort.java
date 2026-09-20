package com.mediawebapp.dto;

import com.mediawebapp.exception.BadRequestException;
import java.util.Locale;

/**
 * Allowed {@code sort} values for GET {@code /api/media}.
 */
public enum DiscoverSort {
	QUALITY,
	YEAR,
	TITLE;

	public static DiscoverSort fromParam(String raw) {
		if (raw == null || raw.isBlank()) {
			return QUALITY;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("Invalid sort. Must be one of: QUALITY, YEAR, TITLE");
		}
	}
}
