package com.mediawebapp.repository;

import java.util.UUID;

/**
 * One row from a native pgvector similarity (or top-rated fallback) query.
 * {@code similarity} is {@code null} for fallback rows that are not vector-ranked.
 */
public record MediaSimilarityRow(
		UUID mediaId,
		String title,
		String mediaType,
		Short releaseYear,
		Double externalRating,
		Integer externalRatingCount,
		String posterUrl,
		Double similarity
) {
	public MediaSimilarityRow(
			UUID mediaId,
			String title,
			String mediaType,
			Short releaseYear,
			Double externalRating,
			Integer externalRatingCount,
			Double similarity) {
		this(mediaId, title, mediaType, releaseYear, externalRating, externalRatingCount, null, similarity);
	}
}
