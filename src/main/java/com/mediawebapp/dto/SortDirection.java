package com.mediawebapp.dto;

import com.mediawebapp.exception.BadRequestException;
import java.util.Locale;

/**
 * Allowed {@code direction} values for GET {@code /api/media}.
 */
public enum SortDirection {
	ASC,
	DESC;

	public static SortDirection fromParam(String raw, DiscoverSort sort) {
		if (raw == null || raw.isBlank()) {
			return sort == DiscoverSort.TITLE ? ASC : DESC;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("Invalid direction. Must be one of: ASC, DESC");
		}
	}
}
