package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.dto.UserMediaRequestDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.dto.UserMediaUpsertResult;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.UserMediaMapper;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.UserMediaRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserMediaServiceTest {

	@Mock
	private UserMediaRepository userMediaRepository;

	@Mock
	private MediaRepository mediaRepository;

	@Mock
	private EntityManager entityManager;

	private UserMediaMapper userMediaMapper;
	private UserMediaService userMediaService;

	private UUID userId;
	private UUID mediaId;
	private UUID mediaTypeId;
	private Media media;
	private MediaType mediaType;

	@BeforeEach
	void setUp() {
		userMediaMapper = new UserMediaMapper();
		userMediaService = new UserMediaService(
				userMediaRepository,
				mediaRepository,
				userMediaMapper,
				entityManager
		);

		userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
		mediaId = UUID.randomUUID();
		mediaTypeId = UUID.randomUUID();

		mediaType = new MediaType();
		mediaType.setId(mediaTypeId);
		mediaType.setName("Movie");

		media = new Media();
		media.setId(mediaId);
		media.setTitle("The Matrix");
		media.setReleaseYear((short) 1999);
		media.setMediaType(mediaType);
	}

	/** First upsert for (user, media) inserts a row and reports created=true. */
	@Test
	void shouldCreateNewEntry() {
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "WATCHING", 8, "Great film");

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.empty());
		when(userMediaRepository.saveAndFlush(any(UserMedia.class))).thenAnswer(invocation -> {
			UserMedia um = invocation.getArgument(0);
			um.setId(UUID.randomUUID());
			um.setCreatedAt(Instant.parse("2026-09-08T10:00:00Z"));
			um.setUpdatedAt(Instant.parse("2026-09-08T10:00:00Z"));
			return um;
		});

		UserMediaUpsertResult result = userMediaService.upsert(userId, request);

		assertThat(result.created()).isTrue();
		assertThat(result.body().status()).isEqualTo(UserMediaStatus.WATCHING);
		assertThat(result.body().rating()).isEqualTo(8);
		assertThat(result.body().review()).isEqualTo("Great film");
		assertThat(result.body().mediaId()).isEqualTo(mediaId);
		assertThat(result.body().title()).isEqualTo("The Matrix");
		assertThat(result.body().mediaType().name()).isEqualTo("Movie");

		ArgumentCaptor<UserMedia> captor = ArgumentCaptor.forClass(UserMedia.class);
		verify(userMediaRepository).saveAndFlush(captor.capture());
		assertThat(captor.getValue().getUserId()).isEqualTo(userId);
		assertThat(captor.getValue().getMedia()).isEqualTo(media);
		verify(entityManager).refresh(any(UserMedia.class));
	}

	/** Upsert with optional rating/review omitted still creates a valid entry. */
	@Test
	void shouldCreateEntryWithNullRatingAndReview() {
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "PLANNED", null, null);

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.empty());
		when(userMediaRepository.saveAndFlush(any(UserMedia.class))).thenAnswer(invocation -> {
			UserMedia um = invocation.getArgument(0);
			um.setId(UUID.randomUUID());
			um.setCreatedAt(Instant.parse("2026-09-08T10:00:00Z"));
			um.setUpdatedAt(Instant.parse("2026-09-08T10:00:00Z"));
			return um;
		});

		UserMediaUpsertResult result = userMediaService.upsert(userId, request);

		assertThat(result.created()).isTrue();
		assertThat(result.body().status()).isEqualTo(UserMediaStatus.PLANNED);
		assertThat(result.body().rating()).isNull();
		assertThat(result.body().review()).isNull();
	}

	/** Second upsert for the same (user, media) updates fields and reports created=false. */
	@Test
	void shouldUpdateExistingEntry() {
		UserMedia existing = existingEntry(UserMediaStatus.PLANNED, null, null);
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "COMPLETED", 9, "Finished it");

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.of(existing));
		when(userMediaRepository.saveAndFlush(any(UserMedia.class))).thenAnswer(invocation -> {
			UserMedia um = invocation.getArgument(0);
			um.setUpdatedAt(Instant.parse("2026-09-08T12:00:00Z"));
			return um;
		});

		UserMediaUpsertResult result = userMediaService.upsert(userId, request);

		assertThat(result.created()).isFalse();
		assertThat(result.body().status()).isEqualTo(UserMediaStatus.COMPLETED);
		assertThat(result.body().rating()).isEqualTo(9);
		assertThat(result.body().review()).isEqualTo("Finished it");
		verify(entityManager).refresh(any(UserMedia.class));
	}

	/** Update with null rating/review clears previously set values (full field replace). */
	@Test
	void shouldClearRatingAndReviewOnUpdate() {
		UserMedia existing = existingEntry(UserMediaStatus.WATCHING, 8, "Great film");
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "DROPPED", null, null);

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.of(existing));
		when(userMediaRepository.saveAndFlush(any(UserMedia.class))).thenAnswer(invocation -> {
			UserMedia um = invocation.getArgument(0);
			um.setUpdatedAt(Instant.parse("2026-09-08T12:00:00Z"));
			return um;
		});

		UserMediaUpsertResult result = userMediaService.upsert(userId, request);

		assertThat(result.created()).isFalse();
		assertThat(result.body().status()).isEqualTo(UserMediaStatus.DROPPED);
		assertThat(result.body().rating()).isNull();
		assertThat(result.body().review()).isNull();
	}

	/**
	 * Concurrent create race: unique constraint violation is recovered by reloading
	 * the row and updating it (created=false).
	 */
	@Test
	void shouldRecoverFromConcurrentCreateRace() {
		UserMedia raced = existingEntry(UserMediaStatus.PLANNED, null, null);
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "WATCHING", 7, "Caught up");

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.empty())
				.thenReturn(Optional.of(raced));
		when(userMediaRepository.saveAndFlush(any(UserMedia.class)))
				.thenThrow(new DataIntegrityViolationException("unique_violation"))
				.thenAnswer(invocation -> {
					UserMedia um = invocation.getArgument(0);
					um.setUpdatedAt(Instant.parse("2026-09-08T12:00:00Z"));
					return um;
				});

		UserMediaUpsertResult result = userMediaService.upsert(userId, request);

		assertThat(result.created()).isFalse();
		assertThat(result.body().status()).isEqualTo(UserMediaStatus.WATCHING);
		assertThat(result.body().rating()).isEqualTo(7);
		assertThat(result.body().review()).isEqualTo("Caught up");
		verify(userMediaRepository, times(2)).findByUserIdAndMediaIdWithMedia(userId, mediaId);
		verify(userMediaRepository, times(2)).saveAndFlush(any(UserMedia.class));
	}

	/** If unique-constraint race recovery cannot reload the row, the original exception is rethrown. */
	@Test
	void shouldRethrowWhenRaceRecoveryCannotReloadEntry() {
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "WATCHING", 7, null);
		DataIntegrityViolationException violation =
				new DataIntegrityViolationException("unique_violation");

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.of(media));
		when(userMediaRepository.findByUserIdAndMediaIdWithMedia(userId, mediaId))
				.thenReturn(Optional.empty());
		when(userMediaRepository.saveAndFlush(any(UserMedia.class))).thenThrow(violation);

		assertThatThrownBy(() -> userMediaService.upsert(userId, request))
				.isSameAs(violation);

		verify(userMediaRepository, times(2)).findByUserIdAndMediaIdWithMedia(userId, mediaId);
	}

	/** Upsert fails with ResourceNotFoundException when the catalog media id does not exist. */
	@Test
	void shouldThrowWhenMediaNotFound() {
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "PLANNED", null, null);

		when(mediaRepository.findByIdWithMediaType(mediaId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> userMediaService.upsert(userId, request))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(mediaId.toString());

		verify(userMediaRepository, never()).saveAndFlush(any());
		verify(userMediaRepository, never()).findByUserIdAndMediaIdWithMedia(any(), any());
	}

	/** Invalid status string fails before any persistence (enum valueOf). */
	@Test
	void shouldThrowWhenStatusInvalid() {
		UserMediaRequestDTO request = new UserMediaRequestDTO(
				mediaId, "NOT_A_STATUS", 5, null);

		assertThatThrownBy(() -> userMediaService.upsert(userId, request))
				.isInstanceOf(IllegalArgumentException.class);

		verify(mediaRepository, never()).findByIdWithMediaType(any());
		verify(userMediaRepository, never()).saveAndFlush(any());
	}

	/** getAllForUser maps every entry for that user into response DTOs. */
	@Test
	void shouldReturnAllForUser() {
		when(userMediaRepository.findAllByUserIdWithMedia(userId))
				.thenReturn(List.of(existingEntry(UserMediaStatus.WATCHING, 7, "Nice")));

		List<UserMediaResponseDTO> results = userMediaService.getAllForUser(userId);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).mediaId()).isEqualTo(mediaId);
		assertThat(results.get(0).status()).isEqualTo(UserMediaStatus.WATCHING);
		verify(userMediaRepository).findAllByUserIdWithMedia(userId);
	}

	/** getAllForUser returns an empty list when the user has no entries. */
	@Test
	void shouldReturnEmptyListWhenUserHasNoEntries() {
		when(userMediaRepository.findAllByUserIdWithMedia(userId)).thenReturn(List.of());

		List<UserMediaResponseDTO> results = userMediaService.getAllForUser(userId);

		assertThat(results).isEmpty();
	}

	/** getAllForUserByStatus returns only entries matching the requested status. */
	@Test
	void shouldReturnFilteredByStatus() {
		when(userMediaRepository.findAllByUserIdAndStatusWithMedia(userId, UserMediaStatus.COMPLETED))
				.thenReturn(List.of(existingEntry(UserMediaStatus.COMPLETED, 10, null)));

		List<UserMediaResponseDTO> results =
				userMediaService.getAllForUserByStatus(userId, UserMediaStatus.COMPLETED);

		assertThat(results).hasSize(1);
		assertThat(results.get(0).status()).isEqualTo(UserMediaStatus.COMPLETED);
		verify(userMediaRepository)
				.findAllByUserIdAndStatusWithMedia(userId, UserMediaStatus.COMPLETED);
	}

	/** Status filter returns empty when the user has no entries with that status. */
	@Test
	void shouldReturnEmptyListWhenNoEntriesMatchStatus() {
		when(userMediaRepository.findAllByUserIdAndStatusWithMedia(userId, UserMediaStatus.DROPPED))
				.thenReturn(List.of());

		List<UserMediaResponseDTO> results =
				userMediaService.getAllForUserByStatus(userId, UserMediaStatus.DROPPED);

		assertThat(results).isEmpty();
	}

	/** deleteForUser removes the entry when it exists for the current user. */
	@Test
	void shouldDeleteSuccessfully() {
		when(userMediaRepository.existsByUserIdAndMedia_Id(userId, mediaId)).thenReturn(true);

		userMediaService.deleteForUser(userId, mediaId);

		verify(userMediaRepository).deleteByUserIdAndMedia_Id(userId, mediaId);
	}

	/** deleteForUser throws when no entry exists for (user, media); delete is not called. */
	@Test
	void shouldThrowWhenDeletingMissingEntry() {
		when(userMediaRepository.existsByUserIdAndMedia_Id(userId, mediaId)).thenReturn(false);

		assertThatThrownBy(() -> userMediaService.deleteForUser(userId, mediaId))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(mediaId.toString());

		verify(userMediaRepository, never()).deleteByUserIdAndMedia_Id(any(), any());
	}

	private UserMedia existingEntry(UserMediaStatus status, Integer rating, String review) {
		UserMedia userMedia = new UserMedia();
		userMedia.setId(UUID.randomUUID());
		userMedia.setUserId(userId);
		userMedia.setMedia(media);
		userMedia.setStatus(status);
		userMedia.setRating(rating);
		userMedia.setReview(review);
		userMedia.setCreatedAt(Instant.parse("2026-09-08T10:00:00Z"));
		userMedia.setUpdatedAt(Instant.parse("2026-09-08T10:00:00Z"));
		return userMedia;
	}
}
