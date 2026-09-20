package com.mediawebapp.service;

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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

	private final MediaRepository mediaRepository;
	private final MediaTypeRepository mediaTypeRepository;
	private final MediaMapper mediaMapper;
	private final EntityManager entityManager;
	private final GenreService genreService;
	private final MediaEmbeddingService mediaEmbeddingService;

	@Transactional
	public MediaResponseDTO createMedia(MediaRequestDTO requestDTO) {
		MediaType mediaType = mediaTypeRepository.findById(requestDTO.mediaTypeId())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media type not found with id: " + requestDTO.mediaTypeId()));

		Media media = mediaMapper.toEntity(requestDTO);
		media.setMediaType(mediaType);
		if (requestDTO.rating() != null || requestDTO.ratingCount() != null) {
			media.setRatingLastUpdatedAt(Instant.now());
		}

		Media savedMedia = mediaRepository.saveAndFlush(media);
		entityManager.refresh(savedMedia);

		savedMedia.getGenres().addAll(genreService.resolveAll(requestDTO.genres()));
		if (!savedMedia.getGenres().isEmpty()) {
			savedMedia = mediaRepository.saveAndFlush(savedMedia);
		}

		scheduleEmbeddingGeneration(savedMedia);
		return mediaMapper.toResponseDto(savedMedia);
	}

	/**
	 * Generates the embedding after the media row is committed so OpenAI HTTP
	 * does not hold the catalog write lock. Failures never fail create/import.
	 */
	private void scheduleEmbeddingGeneration(Media media) {
		UUID mediaId = media.getId();
		String title = media.getTitle();
		String description = media.getDescription();
		List<String> genreNames = media.getGenres().stream().map(Genre::getName).toList();
		Runnable generate = () -> {
			try {
				mediaEmbeddingService.generateAndStore(mediaId, title, description, genreNames);
			} catch (RuntimeException exception) {
				log.error("Failed to generate embedding for media {}: {}", mediaId, exception.getMessage());
			}
		};
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					generate.run();
				}
			});
		} else {
			generate.run();
		}
	}

	@Transactional(readOnly = true)
	public List<MediaResponseDTO> getAllMedia() {
		return mediaRepository.findAllWithMediaType().stream()
				.map(mediaMapper::toResponseDto)
				.toList();
	}

	@Transactional(readOnly = true)
	public MediaResponseDTO getMediaById(UUID id) {
		Media media = mediaRepository.findByIdWithMediaType(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media not found with id: " + id));
		return mediaMapper.toResponseDto(media);
	}
}
