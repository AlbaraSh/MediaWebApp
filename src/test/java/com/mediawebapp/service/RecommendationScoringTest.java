package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationScoringTest {

	@Test
	void weightedAverage_weightsHigherRatingsMore() {
		float[] first = {1f, 0f};
		float[] second = {0f, 1f};

		float[] average = RecommendationScoring.weightedAverage(List.of(
				new RecommendationScoring.WeightedVector(first, 3),
				new RecommendationScoring.WeightedVector(second, 1)));

		assertThat(average).containsExactly(0.75f, 0.25f);
	}

	@Test
	void genreAffinity_isOverlapDividedByCandidateSize() {
		Set<String> liked = Set.of("Action", "Drama");

		assertThat(RecommendationScoring.genreAffinity(liked, List.of("Action", "Comedy")))
				.isEqualTo(0.5);
		assertThat(RecommendationScoring.genreAffinity(liked, List.of())).isZero();
		assertThat(RecommendationScoring.genreAffinity(liked, null)).isZero();
		assertThat(RecommendationScoring.genreAffinity(Set.of(), List.of("Action"))).isZero();
	}

	@Test
	void bayesianQuality_blendsRatingWithGlobalAverage() {
		double quality = RecommendationScoring.bayesianQuality(8.0, 50, 6.0);

		assertThat(quality).isEqualTo(7.0);
		assertThat(RecommendationScoring.bayesianQuality(null, 100, 6.0)).isZero();
		assertThat(RecommendationScoring.bayesianQuality(8.0, null, 6.0)).isZero();
		assertThat(RecommendationScoring.bayesianQuality(8.0, 0, 6.0)).isEqualTo(6.0);
	}

	@Test
	void finalScore_redistributesGenreWeightWhenNoTypeHistory() {
		assertThat(RecommendationScoring.finalScore(1.0, null, 1.0)).isEqualTo(1.0);
		assertThat(RecommendationScoring.finalScore(1.0, 0.0, 0.0)).isEqualTo(0.5);
		assertThat(RecommendationScoring.finalScore(1.0, 1.0, 1.0)).isEqualTo(1.0);
		assertThat(RecommendationScoring.finalScore(0.8, null, 0.4)).isEqualTo(0.74, offset(1e-9));
	}
}
