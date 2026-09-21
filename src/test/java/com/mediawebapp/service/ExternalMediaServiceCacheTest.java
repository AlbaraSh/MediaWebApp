package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.external.adapter.JikanAdapter;
import com.mediawebapp.external.adapter.RawgAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import com.mediawebapp.external.mapper.JikanMapper;
import com.mediawebapp.external.mapper.RawgMapper;
import com.mediawebapp.external.mapper.TmdbMapper;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaExternalIdRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		classes = {
				CacheConfig.class,
				ExternalMediaService.class,
				TmdbMapper.class,
				JikanMapper.class,
				RawgMapper.class,
				MediaMapper.class
		})
@ActiveProfiles("test")
class ExternalMediaServiceCacheTest {

	@Autowired
	private ExternalMediaService service;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private TmdbAdapter tmdbAdapter;
	@MockitoBean
	private JikanAdapter jikanAdapter;
	@MockitoBean
	private RawgAdapter rawgAdapter;
	@MockitoBean
	private MediaService mediaService;
	@MockitoBean
	private MediaTypeRepository mediaTypeRepository;
	@MockitoBean
	private MediaExternalIdRepository mediaExternalIdRepository;
	@MockitoBean
	private TransactionTemplate transactionTemplate;

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
	void search_identicalQueries_hitAdapterOnce() {
		when(tmdbAdapter.searchMovies("matrix")).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(603, "The Matrix", "A computer hacker.", "1999-03-31")
		)));

		List<ExternalMediaDTO> first = service.search("matrix", "MOVIE");
		List<ExternalMediaDTO> second = service.search("matrix", "MOVIE");

		assertThat(second).isEqualTo(first);
		assertThat(first.get(0).externalId()).isEqualTo("movie:603");
		verify(tmdbAdapter, times(1)).searchMovies("matrix");
	}

	@Test
	void search_normalizedKey_isSharedAcrossCaseAndWhitespace() {
		when(tmdbAdapter.searchMovies(" Matrix ")).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(603, "The Matrix", "A computer hacker.", "1999-03-31")
		)));

		service.search(" Matrix ", "MOVIE");
		List<ExternalMediaDTO> hit = service.search("matrix", "movie");

		assertThat(hit).hasSize(1);
		verify(tmdbAdapter, times(1)).searchMovies(" Matrix ");
		verify(tmdbAdapter, never()).searchMovies("matrix");
	}

	@Test
	void search_emptyResults_areCached() {
		when(tmdbAdapter.searchMovies("unknown")).thenReturn(new TmdbMovieSearchResponse(List.of()));

		assertThat(service.search("unknown", "MOVIE")).isEmpty();
		assertThat(service.search("unknown", "MOVIE")).isEmpty();

		verify(tmdbAdapter, times(1)).searchMovies("unknown");
	}

	@Test
	void search_differentQueryOrType_isCacheMiss() {
		when(tmdbAdapter.searchMovies("matrix")).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(603, "The Matrix", "d", "1999-03-31")
		)));
		when(tmdbAdapter.searchMovies("inception")).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(27205, "Inception", "d", "2010-07-16")
		)));
		when(tmdbAdapter.searchTVShows("matrix")).thenReturn(new TmdbTvSearchResponse(List.of(
				new TmdbTv(1, "The Matrix Show", "d", "2020-01-01")
		)));

		service.search("matrix", "MOVIE");
		service.search("inception", "MOVIE");
		service.search("matrix", "TV");

		verify(tmdbAdapter).searchMovies("matrix");
		verify(tmdbAdapter).searchMovies("inception");
		verify(tmdbAdapter).searchTVShows("matrix");
	}

	@Test
	void search_blankQuery_is400AndNotCached() {
		assertThatThrownBy(() -> service.search("  ", "MOVIE"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Query is required");
		assertThatThrownBy(() -> service.search("  ", "MOVIE"))
				.isInstanceOf(BadRequestException.class);

		verifyNoInteractions(tmdbAdapter, jikanAdapter, rawgAdapter);
	}

	@Test
	void search_invalidType_is400AndNotCached() {
		assertThatThrownBy(() -> service.search("matrix", "BOOK"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Invalid type");
		assertThatThrownBy(() -> service.search("matrix", "BOOK"))
				.isInstanceOf(BadRequestException.class);

		verifyNoInteractions(tmdbAdapter, jikanAdapter, rawgAdapter);
	}

	@Test
	void search_providerException_isNotCached() {
		when(tmdbAdapter.searchMovies("matrix"))
				.thenThrow(new ExternalProviderException("TMDB provider is unavailable", true))
				.thenReturn(new TmdbMovieSearchResponse(List.of(
						new TmdbMovie(603, "The Matrix", "d", "1999-03-31")
				)));

		assertThatThrownBy(() -> service.search("matrix", "MOVIE"))
				.isInstanceOf(ExternalProviderException.class);
		assertThat(service.search("matrix", "MOVIE")).hasSize(1);

		verify(tmdbAdapter, times(2)).searchMovies("matrix");
	}

	@Test
	void getByExternalId_identicalCalls_hitAdapterOnce() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));

		ExternalMediaDTO first = service.getByExternalId("tmdb", "movie:550");
		ExternalMediaDTO second = service.getByExternalId("tmdb", "movie:550");

		assertThat(second).isEqualTo(first);
		assertThat(first.externalId()).isEqualTo("movie:550");
		verify(tmdbAdapter, times(1)).getMovie("550");
	}

	@Test
	void getByExternalId_tmdbMovieAndTv_areDifferentKeys() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));
		when(tmdbAdapter.getTVShow("550")).thenReturn(
				new TmdbTv(550, "A Show", "Overview", "2008-01-20"));

		assertThat(service.getByExternalId("tmdb", "movie:550").mediaType()).isEqualTo("Movie");
		assertThat(service.getByExternalId("tmdb", "tv:550").mediaType()).isEqualTo("TV Show");

		verify(tmdbAdapter).getMovie("550");
		verify(tmdbAdapter).getTVShow("550");
	}

	@Test
	void getByExternalIdUncached_alwaysHitsAdapterEvenWhenDetailsCacheIsWarm() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));

		service.getByExternalId("tmdb", "movie:550");
		service.getByExternalId("tmdb", "movie:550");
		service.getByExternalIdUncached("tmdb", "movie:550");

		verify(tmdbAdapter, times(2)).getMovie("550");
	}

	@Test
	void getByExternalId_providerException_isNotCached() {
		when(tmdbAdapter.getMovie("550"))
				.thenThrow(new ExternalProviderException("TMDB provider is unavailable", true))
				.thenReturn(new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));

		assertThatThrownBy(() -> service.getByExternalId("tmdb", "movie:550"))
				.isInstanceOf(ExternalProviderException.class);
		assertThat(service.getByExternalId("tmdb", "movie:550").title()).isEqualTo("Fight Club");

		verify(tmdbAdapter, times(2)).getMovie("550");
	}
}
