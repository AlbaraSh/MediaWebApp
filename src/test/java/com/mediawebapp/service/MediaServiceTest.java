package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.PageResponse;
import com.mediawebapp.entity.Genre;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaQueryRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.repository.UserMediaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaServiceTest {

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private MediaQueryRepository mediaQueryRepository;

	@Mock
	private MediaTypeRepository mediaTypeRepository;

	@Mock
	private EntityManager entityManager;

	@Mock
	private GenreService genreService;

	@Mock
	private MediaEmbeddingService mediaEmbeddingService;

	@Mock
	private UserMediaRepository userMediaRepository;

	private MediaMapper mediaMapper;
	private MediaService mediaService;

	private UUID mediaTypeId;
	private UUID mediaId;
	private MediaType mediaType;

	@BeforeEach
	void setUp() {
		mediaMapper = new MediaMapper();
		mediaService = new MediaService(
				mediaRepository,
				mediaQueryRepository,
				mediaTypeRepository,
				mediaMapper,
				entityManager,
				genreService,
				mediaEmbeddingService,
				userMediaRepository
		);

		mediaTypeId = UUID.randomUUID();
		mediaId = UUID.randomUUID();

		mediaType = new MediaType();
		mediaType.setId(mediaTypeId);
		mediaType.setName("Movie");
		org.mockito.Mockito.lenient().when(genreService.resolveAll(any())).thenReturn(java.util.Set.of());
	}

	/** Valid create request with an existing media type persists and returns a full response DTO. */
	@Test
	void shouldCreateMediaSuccessfully() {
		MediaRequestDTO request = new MediaRequestDTO(
				"The Matrix",
				"A computer hacker learns about reality.",
				(short) 1999,
				mediaTypeId,
				null,
				null,
				null
		);

		when(mediaTypeRepository.findById(mediaTypeId)).thenReturn(Optional.of(mediaType));
		when(mediaRepository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
			Media media = invocation.getArgument(0);
			media.setId(mediaId);
			media.setCreatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			media.setUpdatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			return media;
		});

		MediaResponseDTO response = mediaService.createMedia(request);

		assertThat(response.id()).isEqualTo(mediaId);
		assertThat(response.title()).isEqualTo("The Matrix");
		assertThat(response.description()).isEqualTo("A computer hacker learns about reality.");
		assertThat(response.releaseYear()).isEqualTo((short) 1999);
		assertThat(response.mediaType().id()).isEqualTo(mediaTypeId);
		assertThat(response.mediaType().name()).isEqualTo("Movie");
		assertThat(response.genres()).isEmpty();
		assertThat(response.rating()).isNull();
		assertThat(response.createdAt()).isNotNull();
		assertThat(response.updatedAt()).isNotNull();

		ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
		verify(mediaTypeRepository).findById(mediaTypeId);
		verify(mediaRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getMediaType()).isEqualTo(mediaType);
		verify(entityManager).refresh(any(Media.class));
		verify(mediaEmbeddingService).generateAndStore(
				eq(mediaId),
				eq("The Matrix"),
				eq("A computer hacker learns about reality."),
				any());
	}

	/** Create with optional description and releaseYear omitted still persists successfully. */
	@Test
	void shouldCreateMediaWithNullOptionalFields() {
		MediaRequestDTO request = new MediaRequestDTO(
				"Untitled",
				null,
				null,
				mediaTypeId,
				null,
				null,
				null
		);

		when(mediaTypeRepository.findById(mediaTypeId)).thenReturn(Optional.of(mediaType));
		when(mediaRepository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
			Media media = invocation.getArgument(0);
			media.setId(mediaId);
			media.setCreatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			media.setUpdatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			return media;
		});

		MediaResponseDTO response = mediaService.createMedia(request);

		assertThat(response.title()).isEqualTo("Untitled");
		assertThat(response.description()).isNull();
		assertThat(response.releaseYear()).isNull();
		assertThat(response.mediaType().id()).isEqualTo(mediaTypeId);
		verify(mediaRepository).saveAndFlush(any(Media.class));
	}

	/** Create looks up-or-inserts genres and persists rating fields the same way import does. */
	@Test
	void shouldAttachGenresAndRatingOnCreate() {
		MediaRequestDTO request = new MediaRequestDTO(
				"The Matrix",
				"A computer hacker learns about reality.",
				(short) 1999,
				mediaTypeId,
				List.of("action", "Sci-Fi"),
				8.7,
				18500
		);

		Genre action = new Genre();
		action.setName("Action");
		Genre sciFi = new Genre();
		sciFi.setName("Sci-Fi");
		when(genreService.resolveAll(List.of("action", "Sci-Fi"))).thenReturn(Set.of(action, sciFi));
		when(mediaTypeRepository.findById(mediaTypeId)).thenReturn(Optional.of(mediaType));
		when(mediaRepository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
			Media media = invocation.getArgument(0);
			if (media.getId() == null) {
				media.setId(mediaId);
			}
			media.setCreatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			media.setUpdatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			return media;
		});

		MediaResponseDTO response = mediaService.createMedia(request);

		assertThat(response.genres()).containsExactlyInAnyOrder("Action", "Sci-Fi");
		assertThat(response.rating()).isEqualTo(8.7);
		assertThat(response.ratingCount()).isEqualTo(18500);
		verify(genreService).resolveAll(List.of("action", "Sci-Fi"));
		verify(mediaRepository, times(2)).saveAndFlush(any(Media.class));

		ArgumentCaptor<Media> captor = ArgumentCaptor.forClass(Media.class);
		verify(mediaRepository, times(2)).saveAndFlush(captor.capture());
		assertThat(captor.getAllValues().get(0).getExternalRating()).isEqualTo(8.7);
		assertThat(captor.getAllValues().get(0).getRatingLastUpdatedAt()).isNotNull();
	}

	/** Create fails with ResourceNotFoundException when mediaTypeId does not exist; nothing is saved. */
	@Test
	void shouldThrowWhenMediaTypeNotFound() {
		MediaRequestDTO request = new MediaRequestDTO(
				"The Matrix",
				null,
				(short) 1999,
				mediaTypeId,
				null,
				null,
				null
		);

		when(mediaTypeRepository.findById(mediaTypeId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> mediaService.createMedia(request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(mediaTypeId.toString());

		verify(mediaRepository, never()).saveAndFlush(any(Media.class));
		verify(entityManager, never()).refresh(any());
		verify(mediaEmbeddingService, never()).generateAndStore(any(), any(), any(), any());
	}

	/** discover returns an empty page when the catalog has no matching rows. */
	@Test
	void shouldReturnEmptyDiscoverPageWhenNoMediaExists() {
		when(mediaRepository.findAverageExternalRating()).thenReturn(null);
		when(mediaQueryRepository.countDiscover(null, null, null, null)).thenReturn(0L);

		PageResponse<MediaResponseDTO> response = mediaService.discover(
				Optional.empty(), null, null, null, null, null, null, 0, 20);

		assertThat(response.content()).isEmpty();
		assertThat(response.page()).isZero();
		assertThat(response.size()).isEqualTo(20);
		assertThat(response.totalElements()).isZero();
		assertThat(response.totalPages()).isZero();
		verify(mediaQueryRepository, never()).findDiscoverIds(
				any(), any(), any(), any(), any(), any(), anyDouble(), anyInt(), anyInt(), anyLong());
		verify(userMediaRepository, never()).findMediaIdsByUserIdAndMediaIdIn(any(), any());
	}

	@Test
	void shouldDiscoverPageWithInLibraryFlagsForAuthenticatedUser() {
		Media media = persistedMedia("Inception", (short) 2010);
		UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		when(mediaRepository.findAverageExternalRating()).thenReturn(7.0);
		when(mediaQueryRepository.countDiscover(null, null, null, null)).thenReturn(1L);
		when(mediaQueryRepository.findDiscoverIds(
				any(), any(), any(), any(), any(), any(), anyDouble(), anyInt(), anyInt(), anyLong()))
				.thenReturn(List.of(mediaId));
		when(mediaRepository.findAllWithMediaTypeAndGenresByIdIn(List.of(mediaId))).thenReturn(List.of(media));
		when(userMediaRepository.findMediaIdsByUserIdAndMediaIdIn(userId, List.of(mediaId)))
				.thenReturn(List.of(mediaId));

		PageResponse<MediaResponseDTO> response = mediaService.discover(
				Optional.of(userId), null, null, null, null, null, null, 0, 20);

		assertThat(response.content()).hasSize(1);
		assertThat(response.content().get(0).title()).isEqualTo("Inception");
		assertThat(response.content().get(0).inLibrary()).isTrue();
		assertThat(response.totalElements()).isEqualTo(1);
	}

	@Test
	void shouldLeaveInLibraryNullWhenAnonymous() {
		Media media = persistedMedia("Inception", (short) 2010);
		when(mediaRepository.findAverageExternalRating()).thenReturn(7.0);
		when(mediaQueryRepository.countDiscover(null, null, null, null)).thenReturn(1L);
		when(mediaQueryRepository.findDiscoverIds(
				any(), any(), any(), any(), any(), any(), anyDouble(), anyInt(), anyInt(), anyLong()))
				.thenReturn(List.of(mediaId));
		when(mediaRepository.findAllWithMediaTypeAndGenresByIdIn(List.of(mediaId))).thenReturn(List.of(media));

		PageResponse<MediaResponseDTO> response = mediaService.discover(
				Optional.empty(), null, null, null, null, null, null, 0, 20);

		assertThat(response.content().get(0).inLibrary()).isNull();
		verify(userMediaRepository, never()).findMediaIdsByUserIdAndMediaIdIn(any(), any());
	}

	@Test
	void shouldRejectInvalidDiscoverTypeAndSort() {
		assertThatThrownBy(() -> mediaService.discover(
				Optional.empty(), "BOOK", null, null, null, null, null, 0, 20))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Invalid type");
		assertThatThrownBy(() -> mediaService.discover(
				Optional.empty(), null, null, null, null, "POPULARITY", null, 0, 20))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("Invalid sort");
		assertThatThrownBy(() -> mediaService.discover(
				Optional.empty(), null, null, null, null, null, null, 0, 51))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("size");
	}

	/** getMediaById returns the matching media DTO when the id exists. */
	@Test
	void shouldReturnMediaById() {
		Media media = persistedMedia("Interstellar", (short) 2014);

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));

		MediaResponseDTO response = mediaService.getMediaById(mediaId);

		assertThat(response.id()).isEqualTo(mediaId);
		assertThat(response.title()).isEqualTo("Interstellar");
		assertThat(response.mediaType().id()).isEqualTo(mediaTypeId);

		verify(mediaRepository).findByIdWithMediaType(mediaId);
	}

	/** getMediaById throws ResourceNotFoundException when no media exists for the given id. */
	@Test
	void shouldThrowWhenMediaNotFound() {
		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> mediaService.getMediaById(mediaId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(mediaId.toString());

		verify(mediaRepository).findByIdWithMediaType(mediaId);
	}

	@Test
	void createMedia_succeedsWhenEmbeddingGenerationThrows() {
		MediaRequestDTO request = new MediaRequestDTO(
				"The Matrix",
				null,
				(short) 1999,
				mediaTypeId,
				null,
				null,
				null
		);

		when(mediaTypeRepository.findById(mediaTypeId)).thenReturn(Optional.of(mediaType));
		when(mediaRepository.saveAndFlush(any(Media.class))).thenAnswer(invocation -> {
			Media media = invocation.getArgument(0);
			media.setId(mediaId);
			media.setCreatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			media.setUpdatedAt(Instant.parse("2026-09-07T09:31:11.874953Z"));
			return media;
		});
		doThrow(new RuntimeException("openai down"))
				.when(mediaEmbeddingService)
				.generateAndStore(any(), any(), any(), any());

		MediaResponseDTO response = mediaService.createMedia(request);

		assertThat(response.title()).isEqualTo("The Matrix");
		assertThat(response.id()).isEqualTo(mediaId);
	}

	private Media persistedMedia(String title, Short releaseYear) {
		Media media = new Media();
		media.setId(mediaId);
		media.setTitle(title);
		media.setDescription("Sample description");
		media.setReleaseYear(releaseYear);
		media.setMediaType(mediaType);
		media.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		media.setUpdatedAt(Instant.parse("2026-01-01T00:00:00Z"));
		return media;
	}
}
