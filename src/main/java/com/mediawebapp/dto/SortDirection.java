package com.mediawebapp.dto;

import com.mediawebapp.exception.BadRequestException;
import java.util.Locale;

/** Omitted discover direction sorts title A–Z and every other sort high-to-low. */
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
