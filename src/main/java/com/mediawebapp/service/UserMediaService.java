package com.mediawebapp.service;

import com.mediawebapp.dto.UserMediaRequestDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.dto.UserMediaUpsertResult;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.UserMediaMapper;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.UserMediaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
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
	 * @param userId list owner
	 * @return all entries for the user, with media summaries
	 */
	@Transactional(readOnly = true)
	public List<UserMediaResponseDTO> getAllForUser(UUID userId) {
		return userMediaRepository.findAllByUserIdWithMedia(userId).stream()
				.map(userMediaMapper::toResponseDto)
				.toList();
	}

	/**
	 * @param userId list owner
	 * @param status filter matching the DB enum
	 * @return entries for the user with the given status
	 */
	@Transactional(readOnly = true)
	public List<UserMediaResponseDTO> getAllForUserByStatus(UUID userId, UserMediaStatus status) {
		return userMediaRepository.findAllByUserIdAndStatusWithMedia(userId, status).stream()
				.map(userMediaMapper::toResponseDto)
				.toList();
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
}
