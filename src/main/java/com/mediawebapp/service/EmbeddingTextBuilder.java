package com.mediawebapp.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Builds the text sent to the embedding model.
 * Rating, rating count, and media type are never included.
 */
public final class EmbeddingTextBuilder {

	private EmbeddingTextBuilder() {
	}

	public static String build(String title, String description, Collection<String> genres) {
		List<String> parts = new ArrayList<>();
		addIfPresent(parts, title);
		addIfPresent(parts, description);
		String genrePart = joinGenres(genres);
		addIfPresent(parts, genrePart);
		return String.join(". ", parts);
	}

	private static void addIfPresent(List<String> parts, String value) {
		if (value != null && !value.isBlank()) {
			parts.add(value.trim());
		}
	}

	private static String joinGenres(Collection<String> genres) {
		if (genres == null || genres.isEmpty()) {
			return null;
		}
		List<String> names = new ArrayList<>();
		for (String genre : genres) {
			if (genre != null && !genre.isBlank()) {
				names.add(genre.trim());
			}
		}
		return names.isEmpty() ? null : String.join(", ", names);
	}
}
