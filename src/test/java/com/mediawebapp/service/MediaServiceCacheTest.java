package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaExternalId;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaExternalIdRepository;
import com.mediawebapp.repository.MediaQueryRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.repository.UserMediaRepository;
import com.mediawebapp.schedule.BackoffSleeper;
import com.mediawebapp.schedule.RatingRefreshJob;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = {
				CacheConfig.class,
				MediaService.class,
				MediaMapper.class
		})
@ActiveProfiles("test")
class MediaServiceCacheTest {

	@Autowired
	private MediaService mediaService;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private MediaRepository mediaRepository;
	@MockitoBean
	private MediaQueryRepository mediaQueryRepository;
	@MockitoBean
	private MediaTypeRepository mediaTypeRepository;
	@MockitoBean
	private EntityManager entityManager;
	@MockitoBean
	private GenreService genreService;
	@MockitoBean
	private MediaEmbeddingService mediaEmbeddingService;
	@MockitoBean
	private UserMediaRepository userMediaRepository;
	@MockitoBean
	private ExternalMediaService externalMediaService;
	@MockitoBean
	private MediaExternalIdRepository mediaExternalIdRepository;
	@MockitoBean
	private TransactionTemplate transactionTemplate;
	@MockitoBean
	private BackoffSleeper backoffSleeper;

	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");
	private final UUID unknownId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	@BeforeEach
	void clearCaches() {
		for (String name : CacheConfig.CACHE_NAMES) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				cache.clear();
			}
		}
	}

	@Test
	void getMediaById_identicalCalls_hitRepositoryOnce() {
		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(persistedMedia(7.0, 100)));

		MediaResponseDTO first = mediaService.getMediaById(mediaId);
		MediaResponseDTO second = mediaService.getMediaById(mediaId);

		assertThat(second).isEqualTo(first);
		assertThat(first.inLibrary()).isNull();
		assertThat(second.inLibrary()).isNull();
		verify(mediaRepository, times(1)).findByIdWithMediaType(mediaId);
	}

	@Test
	void getMediaById_unknownId_isNotCached() {
		when(mediaRepository.findByIdWithMediaType(unknownId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> mediaService.getMediaById(unknownId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(unknownId.toString());
		assertThatThrownBy(() -> mediaService.getMediaById(unknownId))
				.isInstanceOf(ResourceNotFoundException.class);

		verify(mediaRepository, times(2)).findByIdWithMediaType(unknownId);
	}

	@Test
	void ratingRefresh_evictsMediaByIdAfterCommit_soNextGetIsNotStale() {
		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(persistedMedia(7.0, 100)));
		assertThat(mediaService.getMediaById(mediaId).rating()).isEqualTo(7.0);

		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(mediaId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(mediaId))
				.thenReturn(Optional.of(mapping()));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550"))
				.thenReturn(new ExternalMediaDTO(
						"Interstellar", "Sample description", (short) 2014, "Movie", "TMDB", "movie:550",
						List.of(), 8.4, 21000));
		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			when(mediaRepository.findById(mediaId)).thenReturn(Optional.of(persistedMedia(7.0, 100)));
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});

		RatingRefreshJob job = new RatingRefreshJob(
				mediaRepository,
				mediaExternalIdRepository,
				externalMediaService,
				transactionTemplate,
				cacheManager,
				backoffSleeper,
				3);
		job.refreshStaleRatings();

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(persistedMedia(8.4, 21000)));

		MediaResponseDTO after = mediaService.getMediaById(mediaId);
		assertThat(after.rating()).isEqualTo(8.4);
		assertThat(after.ratingCount()).isEqualTo(21000);
		assertThat(after.inLibrary()).isNull();
		verify(mediaRepository, times(2)).findByIdWithMediaType(mediaId);
		verify(externalMediaService).getByExternalIdUncached("tmdb", "movie:550");
		verify(externalMediaService, never()).getByExternalId(any(), any());
	}

	private Media persistedMedia(Double rating, Integer count) {
		MediaType mediaType = new MediaType();
		mediaType.setId(mediaTypeId);
		mediaType.setName("Movie");
		Media media = new Media();
		media.setId(mediaId);
		media.setTitle("Interstellar");
		media.setDescription("Sample description");
		media.setReleaseYear((short) 2014);
		media.setMediaType(mediaType);
		media.setExternalRating(rating);
		media.setExternalRatingCount(count);
		media.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		media.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		return media;
	}

	private MediaExternalId mapping() {
		Media media = new Media();
		media.setId(mediaId);
		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(media);
		mapping.setSource("TMDB");
		mapping.setExternalId("movie:550");
		return mapping;
	}
}
