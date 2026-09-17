package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.Genre;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
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
	private MediaTypeRepository mediaTypeRepository;

	@Mock
	private EntityManager entityManager;

	@Mock
	private GenreService genreService;

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
				mediaTypeRepository,
				mediaMapper,
				entityManager,
				genreService
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
	}

	/** getAllMedia maps every persisted entity (with media type) into response DTOs. */
	@Test
	void shouldReturnAllMedia() {
		Media media = persistedMedia("Inception", (short) 2010);

		when(mediaRepository.findAllWithMediaType()).thenReturn(List.of(media));

		List<MediaResponseDTO> responses = mediaService.getAllMedia();

		assertThat(responses).hasSize(1);
		assertThat(responses.get(0).id()).isEqualTo(mediaId);
		assertThat(responses.get(0).title()).isEqualTo("Inception");
		assertThat(responses.get(0).mediaType().name()).isEqualTo("Movie");

		verify(mediaRepository).findAllWithMediaType();
	}

	/** getAllMedia returns an empty list when the catalog has no rows. */
	@Test
	void shouldReturnEmptyListWhenNoMediaExists() {
		when(mediaRepository.findAllWithMediaType()).thenReturn(List.of());

		List<MediaResponseDTO> responses = mediaService.getAllMedia();

		assertThat(responses).isEmpty();
		verify(mediaRepository).findAllWithMediaType();
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
