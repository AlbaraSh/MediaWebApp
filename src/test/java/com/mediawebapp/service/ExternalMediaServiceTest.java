package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaExternalId;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.adapter.JikanAdapter;
import com.mediawebapp.external.adapter.RawgAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.dto.tmdb.TmdbGenre;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class ExternalMediaServiceTest {

	@Mock
	private TmdbAdapter tmdbAdapter;
	@Mock
	private JikanAdapter jikanAdapter;
	@Mock
	private RawgAdapter rawgAdapter;
	@Mock
	private MediaService mediaService;
	@Mock
	private MediaTypeRepository mediaTypeRepository;
	@Mock
	private MediaExternalIdRepository mediaExternalIdRepository;
	@Mock
	private TransactionTemplate transactionTemplate;

	private ExternalMediaService service;

	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");

	@BeforeEach
	void setUp() {
		service = new ExternalMediaService(
				tmdbAdapter,
				jikanAdapter,
				rawgAdapter,
				new TmdbMapper(),
				new JikanMapper(),
				new RawgMapper(),
				mediaService,
				mediaTypeRepository,
				mediaExternalIdRepository,
				new MediaMapper(),
				transactionTemplate
		);
	}

	@Test
	void search_movie_callsTmdbMovieEndpointOnly() {
		when(tmdbAdapter.searchMovies("matrix")).thenReturn(new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(603, "The Matrix", "A computer hacker.", "1999-03-31")
		)));

		List<ExternalMediaDTO> results = service.search("matrix", "MOVIE");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).provider()).isEqualTo("TMDB");
		assertThat(results.get(0).mediaType()).isEqualTo("Movie");
		assertThat(results.get(0).externalId()).isEqualTo("movie:603");
		verify(tmdbAdapter).searchMovies("matrix");
		verify(tmdbAdapter, never()).searchTVShows(any());
		verifyNoInteractions(jikanAdapter, rawgAdapter);
	}

	@Test
	void search_tv_callsTmdbTvEndpointOnly() {
		when(tmdbAdapter.searchTVShows("bad")).thenReturn(new TmdbTvSearchResponse(List.of(
				new TmdbTv(1396, "Breaking Bad", "A chemistry teacher.", "2008-01-20")
		)));

		List<ExternalMediaDTO> results = service.search("bad", "TV");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).externalId()).isEqualTo("tv:1396");
		assertThat(results.get(0).mediaType()).isEqualTo("TV Show");
		verify(tmdbAdapter).searchTVShows("bad");
		verify(tmdbAdapter, never()).searchMovies(any());
	}

	@Test
	void search_limitsResultsTo10() {
		List<TmdbMovie> movies = java.util.stream.IntStream.rangeClosed(1, 15)
				.mapToObj(i -> new TmdbMovie(i, "Title " + i, "d", "2000-01-01"))
				.toList();
		when(tmdbAdapter.searchMovies("a")).thenReturn(new TmdbMovieSearchResponse(movies));

		assertThat(service.search("a", "MOVIE")).hasSize(10);
	}

	@Test
	void search_invalidType_throwsBadRequest() {
		assertThatThrownBy(() -> service.search("x", "BOOK"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Invalid type");
		verifyNoInteractions(tmdbAdapter, jikanAdapter, rawgAdapter);
	}

	@Test
	void getByExternalId_tmdbMovie_usesPrefixedId() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));

		ExternalMediaDTO dto = service.getByExternalId("tmdb", "movie:550");

		assertThat(dto.externalId()).isEqualTo("movie:550");
		assertThat(dto.releaseYear()).isEqualTo((short) 1999);
		verify(tmdbAdapter).getMovie("550");
		verify(tmdbAdapter, never()).getTVShow(any());
	}

	@Test
	void getByExternalId_uppercaseProvider_isRejected() {
		assertThatThrownBy(() -> service.getByExternalId("TMDB", "movie:550"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Invalid provider");
	}

	@Test
	void getByExternalId_tmdbTrimsIdAfterPrefix() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));

		ExternalMediaDTO dto = service.getByExternalId("tmdb", "movie: 550");

		assertThat(dto.externalId()).isEqualTo("movie:550");
		verify(tmdbAdapter).getMovie("550");
	}

	@Test
	void getByExternalId_tmdbWithoutPrefix_throwsBadRequest() {
		assertThatThrownBy(() -> service.getByExternalId("tmdb", "550"))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("movie:");
	}

	@Test
	void import_existingMapping_returns200WithoutCreatingDuplicate() {
		MediaType mediaType = movieType();
		Media existing = persistedMedia("Fight Club", mediaType);
		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(existing);
		mapping.setSource("TMDB");
		mapping.setExternalId("movie:550");

		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.of(mapping));

		ImportMediaResult result = service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		assertThat(result.created()).isFalse();
		assertThat(result.body().id()).isEqualTo(mediaId);
		assertThat(result.body().title()).isEqualTo("Fight Club");
		verifyNoInteractions(tmdbAdapter);
		verify(mediaService, never()).createMedia(any());
		verify(transactionTemplate, never()).execute(any());
		verify(mediaExternalIdRepository, never()).saveAndFlush(any());
	}

	@Test
	void import_newMedia_createsMappingWithUppercaseSource() {
		stubTransactionTemplate();
		MediaType mediaType = movieType();
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.of(mediaType));
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("Fight Club"));

		ImportMediaResult result = service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		assertThat(result.created()).isTrue();
		assertThat(result.body().title()).isEqualTo("Fight Club");

		ArgumentCaptor<MediaRequestDTO> requestCaptor = ArgumentCaptor.forClass(MediaRequestDTO.class);
		verify(mediaService).createMedia(requestCaptor.capture());
		assertThat(requestCaptor.getValue().releaseYear()).isEqualTo((short) 1999);
		assertThat(requestCaptor.getValue().mediaTypeId()).isEqualTo(mediaTypeId);
		assertThat(requestCaptor.getValue().genres()).isEmpty();
		assertThat(requestCaptor.getValue().rating()).isNull();
		assertThat(requestCaptor.getValue().ratingCount()).isNull();

		ArgumentCaptor<MediaExternalId> mappingCaptor = ArgumentCaptor.forClass(MediaExternalId.class);
		verify(mediaExternalIdRepository).saveAndFlush(mappingCaptor.capture());
		assertThat(mappingCaptor.getValue().getSource()).isEqualTo("TMDB");
		assertThat(mappingCaptor.getValue().getExternalId()).isEqualTo("movie:550");
		assertThat(mappingCaptor.getValue().getMedia().getId()).isEqualTo(mediaId);
	}

	@Test
	void import_passesGenresAndRatingIntoCreateRequest() {
		stubTransactionTemplate();
		when(tmdbAdapter.getMovie("550")).thenReturn(new TmdbMovie(
				550,
				"Fight Club",
				"Overview",
				"1999-10-15",
				List.of(new TmdbGenre("Drama")),
				8.4,
				21000,
				"/pB8BM7pdSp9MRfnnWJTFNraSgL.jpg"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.of(movieType()));
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("Fight Club"));

		service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		ArgumentCaptor<MediaRequestDTO> requestCaptor = ArgumentCaptor.forClass(MediaRequestDTO.class);
		verify(mediaService).createMedia(requestCaptor.capture());
		assertThat(requestCaptor.getValue().genres()).containsExactly("Drama");
		assertThat(requestCaptor.getValue().rating()).isEqualTo(8.4);
		assertThat(requestCaptor.getValue().ratingCount()).isEqualTo(21000);
		assertThat(requestCaptor.getValue().posterUrl())
				.isEqualTo("https://image.tmdb.org/t/p/w500/pB8BM7pdSp9MRfnnWJTFNraSgL.jpg");
	}

	@Test
	void import_nullReleaseYear_rejectedBeforeMediaCreation() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", null));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550")))
				.isInstanceOf(BadRequestException.class)
				.hasMessage("Validation failed");

		verify(mediaService, never()).createMedia(any());
		verify(mediaTypeRepository, never()).findByName(any());
		verify(transactionTemplate, never()).execute(any());
	}

	@Test
	void import_releaseYearOutOfRange_rejectedBeforeMediaCreation() {
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Ancient", "Overview", "1700-01-01"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550")))
				.isInstanceOf(BadRequestException.class);

		verify(mediaService, never()).createMedia(any());
		verify(transactionTemplate, never()).execute(any());
	}

	@Test
	void import_mediaTypeMissing_throwsResourceNotFound() {
		stubTransactionTemplate();
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550")))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("Movie");

		verify(mediaService, never()).createMedia(any());
	}

	@Test
	void import_constraintViolation_treatedAsAlreadyImported() {
		stubTransactionTemplate();
		MediaType mediaType = movieType();
		Media existing = persistedMedia("Fight Club", mediaType);
		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(existing);
		mapping.setSource("TMDB");
		mapping.setExternalId("movie:550");

		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty(), Optional.empty(), Optional.of(mapping));
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.of(mediaType));
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("Fight Club"));
		when(mediaExternalIdRepository.saveAndFlush(any()))
				.thenThrow(new DataIntegrityViolationException("unique"));

		ImportMediaResult result = service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		assertThat(result.created()).isFalse();
		assertThat(result.body().id()).isEqualTo(mediaId);
	}

	@Test
	void import_fetchHappensOutsideTransaction() {
		AtomicBoolean insideTransaction = new AtomicBoolean(false);
		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			insideTransaction.set(true);
			TransactionCallback<?> callback = invocation.getArgument(0);
			try {
				return callback.doInTransaction(new SimpleTransactionStatus());
			} finally {
				insideTransaction.set(false);
			}
		});
		when(tmdbAdapter.getMovie("550")).thenAnswer(invocation -> {
			assertThat(insideTransaction.get())
					.as("external fetch must not run inside the DB transaction")
					.isFalse();
			return new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15");
		});
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.of(movieType()));
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("Fight Club"));

		service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		InOrder order = inOrder(tmdbAdapter, transactionTemplate);
		order.verify(tmdbAdapter).getMovie("550");
		order.verify(transactionTemplate).execute(any());
	}

	@Test
	void import_looksUpUppercaseSource() {
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.of(mappingFor(persistedMedia("Fight Club", movieType()))));

		service.importMedia(new ImportMediaRequestDTO("tmdb", "movie:550"));

		verify(mediaExternalIdRepository).findBySourceAndExternalIdWithMedia("TMDB", "movie:550");
		verify(mediaExternalIdRepository, never()).findBySourceAndExternalIdWithMedia(eq("tmdb"), any());
		verifyNoInteractions(tmdbAdapter);
	}

	@Test
	void import_canonicalizesTmdbExternalId() {
		stubTransactionTemplate();
		when(tmdbAdapter.getMovie("550")).thenReturn(
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15"));
		when(mediaExternalIdRepository.findBySourceAndExternalIdWithMedia("TMDB", "movie:550"))
				.thenReturn(Optional.empty());
		when(mediaTypeRepository.findByName("Movie")).thenReturn(Optional.of(movieType()));
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("Fight Club"));

		service.importMedia(new ImportMediaRequestDTO("tmdb", "movie: 550"));

		verify(tmdbAdapter).getMovie("550");
		ArgumentCaptor<MediaExternalId> mappingCaptor = ArgumentCaptor.forClass(MediaExternalId.class);
		verify(mediaExternalIdRepository).saveAndFlush(mappingCaptor.capture());
		assertThat(mappingCaptor.getValue().getExternalId()).isEqualTo("movie:550");
	}

	private void stubTransactionTemplate() {
		when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(new SimpleTransactionStatus());
		});
	}

	private MediaType movieType() {
		MediaType mediaType = new MediaType();
		mediaType.setId(mediaTypeId);
		mediaType.setName("Movie");
		return mediaType;
	}

	private Media persistedMedia(String title, MediaType mediaType) {
		Media media = new Media();
		media.setId(mediaId);
		media.setTitle(title);
		media.setDescription("Overview");
		media.setReleaseYear((short) 1999);
		media.setMediaType(mediaType);
		media.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		media.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		return media;
	}

	private MediaExternalId mappingFor(Media media) {
		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(media);
		mapping.setSource("TMDB");
		mapping.setExternalId("movie:550");
		return mapping;
	}

	private MediaResponseDTO sampleResponse(String title) {
		return new MediaResponseDTO(
				mediaId,
				title,
				"Overview",
				(short) 1999,
				List.of(),
				null,
				null,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z"),
				null
		);
	}
}
