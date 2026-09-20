package com.mediawebapp.repository;

/**
 * Shared PostgreSQL Bayesian quality expression.
 * <p>
 * {@code m} is always bound from {@code RecommendationScoring.BAYESIAN_M}.
 * Rated-row math is identical for Discover ORDER BY and the recommendations
 * top-rated fallback; only the unrated CASE differs (NULL vs 0).
 */
public final class BayesianQualitySql {

	public static final String RATED_EXPRESSION = """
			(m.external_rating_count::float8 / (m.external_rating_count + :m)) * m.external_rating
			     + (:m::float8 / (m.external_rating_count + :m)) * :globalAverage""";

	public static final String CASE_NULL_UNRATED =
			"CASE WHEN m.external_rating IS NULL OR m.external_rating_count IS NULL THEN NULL ELSE "
					+ RATED_EXPRESSION
					+ " END";

	public static final String CASE_ZERO_UNRATED =
			"CASE WHEN m.external_rating IS NULL OR m.external_rating_count IS NULL THEN 0 ELSE "
					+ RATED_EXPRESSION
					+ " END";

	private BayesianQualitySql() {
	}
}
