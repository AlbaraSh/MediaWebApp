package com.mediawebapp.service;

import com.mediawebapp.dto.CatalogType;
import com.mediawebapp.dto.DiscoverSort;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.PageResponse;
import com.mediawebapp.dto.Pagination;
import com.mediawebapp.dto.SortDirection;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
	private final MediaQueryRepository mediaQueryRepository;
	private final MediaTypeRepository mediaTypeRepository;
	private final MediaMapper mediaMapper;
	private final EntityManager entityManager;
	private final GenreService genreService;
	private final MediaEmbeddingService mediaEmbeddingService;
	private final UserMediaRepository userMediaRepository;

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
	public PageResponse<MediaResponseDTO> discover(
			Optional<UUID> currentUserId,
			String type,
			String genre,
			Integer year,
			String q,
			String sort,
			String direction,
			int page,
			int size) {
		Pagination.validate(page, size);
		String typeName = optionalTypeName(type);
		String genreName = canonicalGenre(genre);
		String query = trimToNull(q);
		DiscoverSort discoverSort = DiscoverSort.fromParam(sort);
		SortDirection sortDirection = SortDirection.fromParam(direction, discoverSort);
		double globalAverage = Optional.ofNullable(mediaRepository.findAverageExternalRating()).orElse(0d);

		long totalElements = mediaQueryRepository.countDiscover(typeName, genreName, toYear(year), query);
		List<UUID> ids = totalElements == 0
				? List.of()
				: mediaQueryRepository.findDiscoverIds(
						typeName,
						genreName,
						toYear(year),
						query,
						discoverSort,
						sortDirection,
						globalAverage,
						RecommendationScoring.BAYESIAN_M,
						size,
						Pagination.offset(page, size));
		List<Media> media = loadMediaInOrder(ids);
		Set<UUID> inLibrary = libraryMediaIds(currentUserId, ids);
		boolean anonymous = currentUserId == null || currentUserId.isEmpty();
		List<MediaResponseDTO> content = media.stream()
				.map(item -> mediaMapper.toResponseDto(
						item,
						anonymous ? null : inLibrary.contains(item.getId())))
				.toList();
		return PageResponse.of(content, page, size, totalElements);
	}

	@Transactional(readOnly = true)
	public MediaResponseDTO getMediaById(UUID id) {
		Media media = mediaRepository.findByIdWithMediaType(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media not found with id: " + id));
		return mediaMapper.toResponseDto(media);
	}

	private List<Media> loadMediaInOrder(List<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<UUID, Media> byId = new HashMap<>();
		for (Media media : mediaRepository.findAllWithMediaTypeAndGenresByIdIn(ids)) {
			byId.put(media.getId(), media);
		}
		List<Media> ordered = new ArrayList<>(ids.size());
		for (UUID id : ids) {
			Media media = byId.get(id);
			if (media != null) {
				ordered.add(media);
			}
		}
		return ordered;
	}

	private Set<UUID> libraryMediaIds(Optional<UUID> currentUserId, List<UUID> mediaIds) {
		if (currentUserId == null || currentUserId.isEmpty() || mediaIds.isEmpty()) {
			return Set.of();
		}
		return new HashSet<>(userMediaRepository.findMediaIdsByUserIdAndMediaIdIn(
				currentUserId.get(), mediaIds));
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

	private static String canonicalGenre(String genre) {
		return GenreService.canonicalize(genre);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static Short toYear(Integer year) {
		return year == null ? null : year.shortValue();
	}
}
