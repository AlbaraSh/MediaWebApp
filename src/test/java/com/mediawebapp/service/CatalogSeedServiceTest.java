package com.mediawebapp.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.config.CatalogSeedProperties;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.external.adapter.JikanAdapter;
import com.mediawebapp.external.adapter.RawgAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.dto.jikan.JikanAnime;
import com.mediawebapp.external.dto.jikan.JikanSearchResponse;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.rawg.RawgSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.schedule.BackoffSleeper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogSeedServiceTest {

	@Mock
	private MediaRepository mediaRepository;
	@Mock
	private ExternalMediaService externalMediaService;
	@Mock
	private TmdbAdapter tmdbAdapter;
	@Mock
	private JikanAdapter jikanAdapter;
	@Mock
	private RawgAdapter rawgAdapter;
	@Mock
	private BackoffSleeper backoffSleeper;

	private CatalogSeedService service;

	@BeforeEach
	void setUp() {
		service = new CatalogSeedService(
				new CatalogSeedProperties(true, 1),
				mediaRepository,
				externalMediaService,
				tmdbAdapter,
				jikanAdapter,
				rawgAdapter,
				backoffSleeper);
	}

	@Test
	void seedIfNeeded_skipsWhenCatalogAlreadyFull() {
		when(mediaRepository.count()).thenReturn(4L);

		service.seedIfNeeded();

		verify(tmdbAdapter, never()).listTopRatedMovies(any(Integer.class));
		verify(externalMediaService, never()).importMedia(any());
	}

	@Test
	void seedIfNeeded_importsOneOfEachType() {
		when(mediaRepository.count()).thenReturn(0L, 4L);
		when(tmdbAdapter.listTopRatedMovies(1)).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(603, "The Matrix", "d", "1999-03-31"))));
		when(tmdbAdapter.listTopRatedTv(1)).thenReturn(new TmdbTvSearchResponse(List.of(
				new TmdbTv(1396, "Breaking Bad", "d", "2008-01-20"))));
		when(jikanAdapter.listTopAnime(1)).thenReturn(new JikanSearchResponse(List.of(
				new JikanAnime(1, "Cowboy Bebop", "d", 1998, null))));
		when(rawgAdapter.listTopGames(1)).thenReturn(new RawgSearchResponse(List.of(
				new RawgGame(3498, "GTA V", "2013-09-17", "d"))));
		when(externalMediaService.importMedia(any()))
				.thenReturn(new ImportMediaResult(null, true));

		service.seedIfNeeded();

		verify(externalMediaService).importMedia(new ImportMediaRequestDTO("tmdb", "movie:603"));
		verify(externalMediaService).importMedia(new ImportMediaRequestDTO("tmdb", "tv:1396"));
		verify(externalMediaService).importMedia(new ImportMediaRequestDTO("jikan", "1"));
		verify(externalMediaService).importMedia(new ImportMediaRequestDTO("rawg", "3498"));
	}
}
