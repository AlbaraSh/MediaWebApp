package com.mediawebapp.service;

import com.mediawebapp.dto.CatalogType;
import com.mediawebapp.dto.LibraryPageResponse;
import com.mediawebapp.dto.PageResponse;
import com.mediawebapp.dto.Pagination;
import com.mediawebapp.dto.UserMediaRequestDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.dto.UserMediaStatusCounts;
import com.mediawebapp.dto.UserMediaUpsertResult;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.UserMediaMapper;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.UserMediaQueryRepository;
import com.mediawebapp.repository.UserMediaRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for a user's personal media list (status, rating, review).
 * <p>
 * Operates only on user-specific state; the global media catalog is referenced,
 * never duplicated. Caller supplies {@code userId} — this class never resolves
 * identity.
 */
@Service
@RequiredArgsConstructor
public class UserMediaService {

	private final UserMediaRepository userMediaRepository;
	private final UserMediaQueryRepository userMediaQueryRepository;
	private final MediaRepository mediaRepository;
	private final UserMediaMapper userMediaMapper;
	private final EntityManager entityManager;

	/**
	 * Creates or updates the list entry for {@code (userId, mediaId)}.
	 * <p>
	 * Primary logic is find-then-update-or-create. A unique-constraint race is
	 * handled by reloading and updating so the DB constraint stays a safety net.
	 *
	 * @param userId     owner of the list (from CurrentUserProvider via controller)
	 * @param requestDTO validated upsert payload
	 * @return response body plus whether the row was newly created
	 */
	@Transactional
	public UserMediaUpsertResult upsert(UUID userId, UserMediaRequestDTO requestDTO) {
		UserMediaStatus status = UserMediaStatus.valueOf(requestDTO.status());

		Media media = mediaRepository.findByIdWithMediaType(requestDTO.mediaId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media not found with id: " + requestDTO.mediaId()));

		Optional<UserMedia> existing = userMediaRepository
				.findByUserIdAndMediaIdWithMedia(userId, requestDTO.mediaId());

		if (existing.isPresent()) {
			return new UserMediaUpsertResult(updateEntry(existing.get(), status, requestDTO), false);
		}

		try {
			return new UserMediaUpsertResult(createEntry(userId, media, status, requestDTO), true);
		} catch (DataIntegrityViolationException ex) {
			UserMedia raced = userMediaRepository
					.findByUserIdAndMediaIdWithMedia(userId, requestDTO.mediaId())
					.orElseThrow(() -> ex);
			return new UserMediaUpsertResult(updateEntry(raced, status, requestDTO), false);
		}
	}

	/**
	 * Lists the current user's shelf as a page. Default status is COMPLETED.
	 * Sort is fixed: user rating DESC NULLS LAST, updated_at DESC, media id ASC.
	 */
	@Transactional(readOnly = true)
	public LibraryPageResponse listForUser(
			UUID userId,
			UserMediaStatus status,
			String type,
			String genre,
			Integer minRating,
			Integer maxRating,
			String q,
			int page,
			int size) {
		Pagination.validate(page, size);
		validateRatingBounds(minRating, maxRating);
		UserMediaStatus effectiveStatus = status == null ? UserMediaStatus.COMPLETED : status;
		String typeName = optionalTypeName(type);
		String genreName = GenreService.canonicalize(genre);
		String query = trimToNull(q);

		long totalElements = userMediaQueryRepository.countLibrary(
				userId, effectiveStatus, typeName, genreName, minRating, maxRating, query);
		List<UUID> ids = totalElements == 0
				? List.of()
				: userMediaQueryRepository.findLibraryIds(
						userId,
						effectiveStatus,
						typeName,
						genreName,
						minRating,
						maxRating,
						query,
						size,
						Pagination.offset(page, size));
		List<UserMediaResponseDTO> content = loadEntriesInOrder(ids).stream()
				.map(userMediaMapper::toResponseDto)
				.toList();
		UserMediaStatusCounts counts = userMediaQueryRepository.countByStatus(
				userId, typeName, genreName, query);
		return LibraryPageResponse.of(PageResponse.of(content, page, size, totalElements), counts);
	}

	/**
	 * Removes the entry for {@code (userId, mediaId)}.
	 *
	 * @throws ResourceNotFoundException if no such entry exists
	 */
	@Transactional
	public void deleteForUser(UUID userId, UUID mediaId) {
		if (!userMediaRepository.existsByUserIdAndMedia_Id(userId, mediaId)) {
			throw new ResourceNotFoundException(
					"User media entry not found for media id: " + mediaId);
		}
		userMediaRepository.deleteByUserIdAndMedia_Id(userId, mediaId);
	}

	private UserMediaResponseDTO createEntry(
			UUID userId,
			Media media,
			UserMediaStatus status,
			UserMediaRequestDTO requestDTO) {
		UserMedia userMedia = new UserMedia();
		userMedia.setUserId(userId);
		userMedia.setMedia(media);
		userMedia.setStatus(status);
		userMedia.setRating(requestDTO.rating());
		userMedia.setReview(requestDTO.review());

		UserMedia saved = userMediaRepository.saveAndFlush(userMedia);
		entityManager.refresh(saved);
		return userMediaMapper.toResponseDto(saved);
	}

	private UserMediaResponseDTO updateEntry(
			UserMedia userMedia,
			UserMediaStatus status,
			UserMediaRequestDTO requestDTO) {
		userMedia.setStatus(status);
		userMedia.setRating(requestDTO.rating());
		userMedia.setReview(requestDTO.review());

		UserMedia saved = userMediaRepository.saveAndFlush(userMedia);
		entityManager.refresh(saved);
		return userMediaMapper.toResponseDto(saved);
	}

	private List<UserMedia> loadEntriesInOrder(List<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<UUID, UserMedia> byId = new HashMap<>();
		for (UserMedia entry : userMediaRepository.findAllByIdInWithMedia(ids)) {
			byId.put(entry.getId(), entry);
		}
		List<UserMedia> ordered = new ArrayList<>(ids.size());
		for (UUID id : ids) {
			UserMedia entry = byId.get(id);
			if (entry != null) {
				ordered.add(entry);
			}
		}
		return ordered;
	}

	private static void validateRatingBounds(Integer minRating, Integer maxRating) {
		if (minRating != null && (minRating < 1 || minRating > 10)) {
			throw new BadRequestException("minRating must be between 1 and 10");
		}
		if (maxRating != null && (maxRating < 1 || maxRating > 10)) {
			throw new BadRequestException("maxRating must be between 1 and 10");
		}
		if (minRating != null && maxRating != null && minRating > maxRating) {
			throw new BadRequestException("minRating must be less than or equal to maxRating");
		}
	}

	private static String optionalTypeName(String type) {
		if (type == null || type.isBlank()) {
			return null;
		}
		return CatalogType.fromParam(type)
				.orElseThrow(() -> new BadRequestException(
						"Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"))
				.mediaTypeName();
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
