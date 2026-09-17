package com.mediawebapp.external.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * RAWG ratings are 0–5; catalog and other providers use 0–10.
 */
public final class RawgRatingConverter {

	private RawgRatingConverter() {
	}

	public static Double toTenPointScale(Double rawgRating) {
		if (rawgRating == null) {
			return null;
		}
		return BigDecimal.valueOf(rawgRating)
				.multiply(BigDecimal.valueOf(2))
				.setScale(1, RoundingMode.HALF_UP)
				.doubleValue();
	}
}
