package com.mediawebapp.service;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service for media catalog operations.
 * <p>
 * Owns all business rules for creating and reading media: resolving media
 * types, persisting entities, and returning API DTOs. Controllers call this
 * class only — they never touch repositories or perform mapping themselves.
 * <p>
 * This layer is the extension point for future features (ingestion, embeddings,
 * recommendations) without changing the HTTP contract prematurely.
 */
@Service
@RequiredArgsConstructor
public class MediaService {

	private final MediaRepository mediaRepository;
	private final MediaTypeRepository mediaTypeRepository;
	private final MediaMapper mediaMapper;
	private final EntityManager entityManager;

	/**
	 * Creates a new media row from a validated request DTO.
	 * <p>
	 * Resolves {@code mediaTypeId} against {@code media_types} before insert so
	 * clients get a clear 404 when the type is missing, instead of a raw FK
	 * constraint error. After insert, the entity is refreshed so database
	 * defaults ({@code created_at}, {@code updated_at}) appear in the response.
	 *
	 * @param requestDTO validated create payload
	 * @return persisted media as an API response DTO
	 * @throws ResourceNotFoundException if the referenced media type does not exist
	 */
	@Transactional
	public MediaResponseDTO createMedia(MediaRequestDTO requestDTO) {
		MediaType mediaType = mediaTypeRepository.findById(requestDTO.mediaTypeId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media type not found with id: " + requestDTO.mediaTypeId()));

		Media media = mediaMapper.toEntity(requestDTO);
		media.setMediaType(mediaType);

		Media savedMedia = mediaRepository.saveAndFlush(media);
		entityManager.refresh(savedMedia);

		return mediaMapper.toResponseDto(savedMedia);
	}

	/**
	 * Returns every media item with its media type eagerly loaded.
	 * <p>
	 * Uses a join-fetch query so mapping to {@link MediaResponseDTO} does not
	 * trigger lazy-loading outside the transaction ({@code open-in-view} is off).
	 *
	 * @return list of media response DTOs (empty if none exist)
	 */
	@Transactional(readOnly = true)
	public List<MediaResponseDTO> getAllMedia() {
		return mediaRepository.findAllWithMediaType().stream()
				.map(mediaMapper::toResponseDto)
				.toList();
	}

	/**
	 * Loads a single media item by primary key.
	 *
	 * @param id media UUID from the path
	 * @return media response DTO
	 * @throws ResourceNotFoundException if no row exists for {@code id}
	 */
	@Transactional(readOnly = true)
	public MediaResponseDTO getMediaById(UUID id) {
		Media media = mediaRepository.findByIdWithMediaType(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media not found with id: " + id));
		return mediaMapper.toResponseDto(media);
	}
}
