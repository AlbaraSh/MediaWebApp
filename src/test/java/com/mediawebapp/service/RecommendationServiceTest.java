package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.RecommendationItemDTO;
import com.mediawebapp.dto.RecommendationResponseDTO;
import com.mediawebapp.entity.Genre;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.repository.MediaEmbeddingRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaSimilarityRow;
import com.mediawebapp.repository.UserMediaRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private MediaEmbeddingRepository mediaEmbeddingRepository;

	@Mock
	private UserMediaRepository userMediaRepository;

	private RecommendationService recommendationService;
	private UUID mediaId;
	private UUID userId;

	@BeforeEach
	void setUp() {
		recommendationService = new RecommendationService(
				mediaRepository, mediaEmbeddingRepository, userMediaRepository);
		mediaId = UUID.randomUUID();
		userId = UUID.randomUUID();
	}

	@Test
	void findSimilar_throwsWhenMediaMissing() {
		when(mediaRepository.existsById(mediaId)).thenReturn(false);

		assertThatThrownBy(() -> recommendationService.findSimilar(mediaId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(mediaId.toString());
		verify(mediaEmbeddingRepository, never()).findSimilar(any(), any(), anyInt());
	}

	@Test
	void findSimilar_throwsWhenEmbeddingMissing() {
		when(mediaRepository.existsById(mediaId)).thenReturn(true);
		when(mediaEmbeddingRepository.findEmbeddingLiteral(mediaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> recommendationService.findSimilar(mediaId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("not yet recommendable");
		verify(mediaEmbeddingRepository, never()).findSimilar(any(), any(), anyInt());
	}

	@Test
	void recommendForUser_rejectsInvalidType() {
		assertThatThrownBy(() -> recommendationService.recommendForUser(userId, "BOOK"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("MOVIE, TV, ANIME, GAME");
		assertThatThrownBy(() -> recommendationService.recommendForUser(userId, " "))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Type is required");
	}

	@Test
	void recommendForUser_fallsBackWhenNoPositiveEmbeddings() {
		when(userMediaRepository.findAllByUserIdAndRatingGreaterThanEqualWithMedia(userId, 7))
				.thenReturn(List.of());
		when(mediaRepository.findAverageExternalRating()).thenReturn(7.0);
		when(mediaEmbeddingRepository.findTopRatedByType(eq("Movie"), eq(userId), eq(7.0), eq(50), eq(20)))
				.thenReturn(List.of(row(UUID.randomUUID(), "Movie", "Heat")));
		when(mediaEmbeddingRepository.findTopRatedByType(eq("TV Show"), eq(userId), eq(7.0), eq(50), eq(5)))
				.thenReturn(List.of());
		when(mediaEmbeddingRepository.findTopRatedByType(eq("Anime"), eq(userId), eq(7.0), eq(50), eq(5)))
				.thenReturn(List.of());
		when(mediaEmbeddingRepository.findTopRatedByType(eq("Game"), eq(userId), eq(7.0), eq(50), eq(5)))
				.thenReturn(List.of());

		RecommendationResponseDTO response = recommendationService.recommendForUser(userId, "MOVIE");

		assertThat(response.movies()).extracting(RecommendationItemDTO::title).containsExactly("Heat");
		assertThat(response.tvShows()).isEmpty();
		verify(mediaEmbeddingRepository, never()).findSimilarByType(any(), any(), any(), anyInt());
	}

	@Test
	void recommendForUser_omitsGenreAffinityWhenTypeHasNoHistory() {
		UUID likedMovieId = UUID.randomUUID();
		UUID similarTvId = UUID.randomUUID();
		UUID actionTvId = UUID.randomUUID();
		UserMedia liked = positiveMovie(likedMovieId, "Action");
		float[] embedding = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		embedding[0] = 1f;

		when(userMediaRepository.findAllByUserIdAndRatingGreaterThanEqualWithMedia(userId, 7))
				.thenReturn(List.of(liked));
		when(mediaEmbeddingRepository.findEmbeddingsByMediaIds(List.of(likedMovieId)))
				.thenReturn(java.util.Map.of(likedMovieId, embedding));
		when(mediaRepository.findAverageExternalRating()).thenReturn(5.0);
		when(mediaEmbeddingRepository.findSimilarByType(any(), eq("Movie"), eq(userId), eq(50)))
				.thenReturn(List.of());
		when(mediaEmbeddingRepository.findSimilarByType(any(), eq("TV Show"), eq(userId), eq(50)))
				.thenReturn(List.of(
						new MediaSimilarityRow(actionTvId, "Action TV", "TV Show", (short) 2020, 8.0, 100, 0.80),
						new MediaSimilarityRow(similarTvId, "Similar TV", "TV Show", (short) 2021, 8.0, 100, 0.90)));
		when(mediaEmbeddingRepository.findSimilarByType(any(), eq("Anime"), eq(userId), eq(50)))
				.thenReturn(List.of());
		when(mediaEmbeddingRepository.findSimilarByType(any(), eq("Game"), eq(userId), eq(50)))
				.thenReturn(List.of());

		Media actionTv = media(actionTvId, "TV Show", "Action");
		Media similarTv = media(similarTvId, "TV Show", "Comedy");
		when(mediaRepository.findAllWithGenresByIdIn(any())).thenReturn(List.of(actionTv, similarTv));

		RecommendationResponseDTO response = recommendationService.recommendForUser(userId, "TV");

		assertThat(response.tvShows()).extracting(RecommendationItemDTO::title)
				.containsExactly("Similar TV", "Action TV");
	}

	private UserMedia positiveMovie(UUID id, String genreName) {
		UserMedia userMedia = new UserMedia();
		userMedia.setRating(10);
		userMedia.setMedia(media(id, "Movie", genreName));
		return userMedia;
	}

	private Media media(UUID id, String typeName, String genreName) {
		MediaType type = new MediaType();
		type.setName(typeName);
		Genre genre = new Genre();
		genre.setName(genreName);
		Media media = new Media();
		media.setId(id);
		media.setTitle(typeName + "-" + id);
		media.setMediaType(type);
		media.setGenres(Set.of(genre));
		return media;
	}

	private MediaSimilarityRow row(UUID id, String type, String title) {
		return new MediaSimilarityRow(id, title, type, (short) 1995, 8.5, 1000, null);
	}
}
