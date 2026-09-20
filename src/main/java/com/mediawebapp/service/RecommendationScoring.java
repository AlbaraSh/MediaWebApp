package com.mediawebapp.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Pure scoring helpers for personalized recommendations.
 * Candidate cosine ranking stays in PostgreSQL; this only combines already-fetched signals.
 */
public final class RecommendationScoring {

	public static final int EMBEDDING_DIMENSIONS = 1536;
	public static final int BAYESIAN_M = 50;
	public static final double SIMILARITY_WEIGHT = 0.5;
	public static final double GENRE_WEIGHT = 0.35;
	public static final double QUALITY_WEIGHT = 0.15;
	public static final double NO_HISTORY_SIMILARITY_WEIGHT = SIMILARITY_WEIGHT + GENRE_WEIGHT;

	private RecommendationScoring() {
	}

	public record WeightedVector(float[] vector, double weight) {
	}

	public static float[] weightedAverage(List<WeightedVector> items) {
		if (items == null || items.isEmpty()) {
			throw new IllegalArgumentException("Cannot average an empty embedding set");
		}
		int dimensions = items.get(0).vector().length;
		double[] accumulator = new double[dimensions];
		double weightSum = 0;
		for (WeightedVector item : items) {
			if (item.vector() == null || item.vector().length != dimensions || item.weight() <= 0) {
				continue;
			}
			weightSum += item.weight();
			for (int i = 0; i < dimensions; i++) {
				accumulator[i] += item.vector()[i] * item.weight();
			}
		}
		if (weightSum == 0) {
			throw new IllegalArgumentException("Cannot average embeddings with zero total weight");
		}
		float[] average = new float[dimensions];
		for (int i = 0; i < dimensions; i++) {
			average[i] = (float) (accumulator[i] / weightSum);
		}
		return average;
	}

	public static double genreAffinity(Set<String> likedGenres, Collection<String> candidateGenres) {
		if (candidateGenres == null || candidateGenres.isEmpty()) {
			return 0;
		}
		if (likedGenres == null || likedGenres.isEmpty()) {
			return 0;
		}
		long overlap = 0;
		for (String genre : candidateGenres) {
			if (genre != null && likedGenres.contains(genre)) {
				overlap++;
			}
		}
		return (double) overlap / candidateGenres.size();
	}

	public static double bayesianQuality(Double rating, Integer ratingCount, double globalAverage) {
		if (rating == null || ratingCount == null || ratingCount < 0) {
			return 0;
		}
		double v = ratingCount;
		double m = BAYESIAN_M;
		double denominator = v + m;
		if (denominator == 0) {
			return 0;
		}
		return (v / denominator) * rating + (m / denominator) * globalAverage;
	}

	public static double finalScore(double similarity, Double genreAffinity, double quality) {
		if (genreAffinity == null) {
			return (NO_HISTORY_SIMILARITY_WEIGHT * similarity) + (QUALITY_WEIGHT * quality);
		}
		return (SIMILARITY_WEIGHT * similarity)
				+ (GENRE_WEIGHT * genreAffinity)
				+ (QUALITY_WEIGHT * quality);
	}
}
