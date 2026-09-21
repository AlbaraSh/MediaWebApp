package com.mediawebapp.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaExternalId;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.repository.MediaExternalIdRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.service.ExternalMediaService;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class RatingRefreshJobTest {

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private MediaExternalIdRepository mediaExternalIdRepository;

	@Mock
	private ExternalMediaService externalMediaService;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private CacheManager cacheManager;

	@Mock
	private Cache mediaByIdCache;

	@Mock
	private BackoffSleeper backoffSleeper;

	private RatingRefreshJob job;

	private final UUID staleId = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID secondId = UUID.fromString("22222222-2222-2222-2222-222222222222");
	private final UUID freshId = UUID.fromString("33333333-3333-3333-3333-333333333333");

	@BeforeEach
	void setUp() {
		job = new RatingRefreshJob(
				mediaRepository,
				mediaExternalIdRepository,
				externalMediaService,
				transactionTemplate,
				cacheManager,
				backoffSleeper,
				3);
		org.mockito.Mockito.lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});
		org.mockito.Mockito.lenient().when(cacheManager.getCache(CacheConfig.MEDIA_BY_ID))
				.thenReturn(mediaByIdCache);
	}

	@Test
	void refresh_onlyProcessesIdsReturnedAsStale() {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:550")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550")).thenReturn(tmdbDto(8.4, 21000));
		when(mediaRepository.findById(staleId)).thenReturn(Optional.of(media(staleId, 7.0, 100)));

		job.refreshStaleRatings();

		verify(mediaExternalIdRepository).findFirstByMedia_Id(staleId);
		verify(mediaExternalIdRepository, never()).findFirstByMedia_Id(freshId);
		verify(externalMediaService).getByExternalIdUncached("tmdb", "movie:550");
		verify(externalMediaService, never()).getByExternalId(any(), any());
		verify(mediaByIdCache).evict(staleId);

		ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(staleId);
		assertThat(saved.getValue().getExternalRating()).isEqualTo(8.4);
		assertThat(saved.getValue().getExternalRatingCount()).isEqualTo(21000);
		assertThat(saved.getValue().getRatingLastUpdatedAt()).isNotNull();
	}

	@Test
	void refresh_storesConvertedRawgRatingFromFetchDto() {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "RAWG", "3498")));
		when(externalMediaService.getByExternalIdUncached("rawg", "3498")).thenReturn(rawgDto(8.9, 4200));
		when(mediaRepository.findById(staleId)).thenReturn(Optional.of(media(staleId, null, null)));

		job.refreshStaleRatings();

		ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository).save(saved.capture());
		assertThat(saved.getValue().getExternalRating()).isEqualTo(8.9);
		assertThat(saved.getValue().getExternalRatingCount()).isEqualTo(4200);
	}

	@Test
	void refresh_retriesTransientFailureThenSucceeds() throws Exception {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:550")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550"))
				.thenThrow(new ExternalProviderException("timeout", true))
				.thenThrow(new ExternalProviderException("429", true))
				.thenThrow(new ExternalProviderException("502", true))
				.thenReturn(tmdbDto(8.1, 10));
		when(mediaRepository.findById(staleId)).thenReturn(Optional.of(media(staleId, 7.0, 1)));

		job.refreshStaleRatings();

		verify(externalMediaService, times(4)).getByExternalIdUncached("tmdb", "movie:550");
		verify(backoffSleeper).sleep(Duration.ofSeconds(1));
		verify(backoffSleeper).sleep(Duration.ofSeconds(2));
		verify(backoffSleeper).sleep(Duration.ofSeconds(4));
		verify(mediaRepository).save(any(Media.class));
	}

	@Test
	void refresh_givesUpAfterFourTransientFailuresWithoutUpdatingRow() throws Exception {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:550")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550"))
				.thenThrow(new ExternalProviderException("timeout", true));

		job.refreshStaleRatings();

		verify(externalMediaService, times(4)).getByExternalIdUncached("tmdb", "movie:550");
		verify(backoffSleeper, times(3)).sleep(any());
		verify(mediaRepository, never()).save(any());
		verify(mediaRepository, never()).findById(any());
		verify(mediaByIdCache, never()).evict(any());
	}

	@Test
	void refresh_doesNotRetryOn404() throws Exception {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:550")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550"))
				.thenThrow(new ResourceNotFoundException("not found"));

		job.refreshStaleRatings();

		verify(externalMediaService, times(1)).getByExternalIdUncached("tmdb", "movie:550");
		verify(backoffSleeper, never()).sleep(any());
		verify(mediaRepository, never()).save(any());
		verify(mediaByIdCache, never()).evict(any());
	}

	@Test
	void refresh_doesNotRetryOnNonRetryableProviderError() throws Exception {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "JIKAN", "1")));
		when(externalMediaService.getByExternalIdUncached("jikan", "1"))
				.thenThrow(new ExternalProviderException("400 from provider", false));

		job.refreshStaleRatings();

		verify(externalMediaService, times(1)).getByExternalIdUncached("jikan", "1");
		verify(backoffSleeper, never()).sleep(any());
		verify(mediaRepository, never()).save(any());
		verify(mediaByIdCache, never()).evict(any());
	}

	@Test
	void refresh_continuesAfterOneRowFails() {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId, secondId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:1")));
		when(mediaExternalIdRepository.findFirstByMedia_Id(secondId)).thenReturn(Optional.of(mapping(secondId, "RAWG", "3498")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:1"))
				.thenThrow(new ExternalProviderException("timeout", true));
		when(externalMediaService.getByExternalIdUncached("rawg", "3498")).thenReturn(rawgDto(8.9, 50));
		when(mediaRepository.findById(secondId)).thenReturn(Optional.of(media(secondId, 4.0, 10)));

		job.refreshStaleRatings();

		verify(externalMediaService, times(4)).getByExternalIdUncached("tmdb", "movie:1");
		verify(externalMediaService).getByExternalIdUncached("rawg", "3498");
		ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(secondId);
		assertThat(saved.getValue().getExternalRating()).isEqualTo(8.9);
	}

	@Test
	void refresh_unexpectedExceptionOnOneRowDoesNotStopOthers() {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId, secondId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId))
				.thenThrow(new IllegalStateException("mapping query failed"));
		when(mediaExternalIdRepository.findFirstByMedia_Id(secondId))
				.thenReturn(Optional.of(mapping(secondId, "RAWG", "3498")));
		when(externalMediaService.getByExternalIdUncached("rawg", "3498")).thenReturn(rawgDto(8.9, 50));
		when(mediaRepository.findById(secondId)).thenReturn(Optional.of(media(secondId, 4.0, 10)));

		job.refreshStaleRatings();

		ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository).save(saved.capture());
		assertThat(saved.getValue().getId()).isEqualTo(secondId);
	}

	@Test
	void refresh_skipsRowWithoutExternalMapping() {
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId)).thenReturn(Optional.empty());

		job.refreshStaleRatings();

		verify(externalMediaService, never()).getByExternalIdUncached(any(), any());
		verify(externalMediaService, never()).getByExternalId(any(), any());
		verify(mediaRepository, never()).save(any());
		verify(mediaByIdCache, never()).evict(any());
	}

	@Test
	void refresh_evictsMediaByIdOnlyAfterWriteTransactionReturns() {
		AtomicBoolean executeReturned = new AtomicBoolean(false);
		when(mediaRepository.findIdsWithStaleRatings(any())).thenReturn(List.of(staleId));
		when(mediaExternalIdRepository.findFirstByMedia_Id(staleId))
				.thenReturn(Optional.of(mapping(staleId, "TMDB", "movie:550")));
		when(externalMediaService.getByExternalIdUncached("tmdb", "movie:550")).thenReturn(tmdbDto(8.4, 21000));
		when(mediaRepository.findById(staleId)).thenReturn(Optional.of(media(staleId, 7.0, 100)));
		org.mockito.Mockito.doAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			Object result = callback.doInTransaction(new SimpleTransactionStatus());
			executeReturned.set(true);
			return result;
		}).when(transactionTemplate).execute(any());
		org.mockito.Mockito.doAnswer(invocation -> {
			assertThat(executeReturned.get())
					.as("media-by-id must be evicted after TransactionTemplate.execute returns")
					.isTrue();
			return null;
		}).when(mediaByIdCache).evict(staleId);

		job.refreshStaleRatings();

		verify(externalMediaService).getByExternalIdUncached("tmdb", "movie:550");
		verify(externalMediaService, never()).getByExternalId(any(), any());
		verify(mediaByIdCache).evict(staleId);
		assertThat(executeReturned.get()).isTrue();
	}

	private static MediaExternalId mapping(UUID mediaId, String source, String externalId) {
		Media media = new Media();
		media.setId(mediaId);
		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(media);
		mapping.setSource(source);
		mapping.setExternalId(externalId);
		return mapping;
	}

	private static Media media(UUID id, Double rating, Integer count) {
		Media media = new Media();
		media.setId(id);
		media.setTitle("Item");
		media.setExternalRating(rating);
		media.setExternalRatingCount(count);
		return media;
	}

	private static ExternalMediaDTO tmdbDto(Double rating, Integer count) {
		return new ExternalMediaDTO(
				"Fight Club", "Overview", (short) 1999, "Movie", "TMDB", "movie:550",
				List.of("Drama"), rating, count);
	}

	private static ExternalMediaDTO rawgDto(Double rating, Integer count) {
		return new ExternalMediaDTO(
				"GTA V", "Synopsis", (short) 2013, "Game", "RAWG", "3498",
				List.of("Action"), rating, count);
	}
}
