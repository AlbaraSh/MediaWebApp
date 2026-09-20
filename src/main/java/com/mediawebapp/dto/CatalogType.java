package com.mediawebapp.dto;

import java.util.Locale;
import java.util.Optional;

/**
 * API {@code type} query values mapped to seeded {@code media_types.name} rows.
 */
public enum CatalogType {
	MOVIE("Movie"),
	TV("TV Show"),
	ANIME("Anime"),
	GAME("Game");

	private final String mediaTypeName;

	CatalogType(String mediaTypeName) {
		this.mediaTypeName = mediaTypeName;
	}

	public String mediaTypeName() {
		return mediaTypeName;
	}

	public static Optional<CatalogType> fromParam(String raw) {
		if (raw == null || raw.isBlank()) {
			return Optional.empty();
		}
		try {
			return Optional.of(valueOf(raw.trim().toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException exception) {
			return Optional.empty();
		}
	}
}
