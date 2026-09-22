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
		return fromParam(raw, sort == DiscoverSort.TITLE ? ASC : DESC);
	}

	public static SortDirection fromParam(String raw, SortDirection whenOmitted) {
		if (raw == null || raw.isBlank()) {
			return whenOmitted;
		}
		try {
			return valueOf(raw.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			throw new BadRequestException("Invalid direction. Must be one of: ASC, DESC");
		}
	}
}
