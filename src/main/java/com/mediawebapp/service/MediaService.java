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
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MediaService {

	private final MediaRepository mediaRepository;
	private final MediaTypeRepository mediaTypeRepository;
	private final MediaMapper mediaMapper;
	private final EntityManager entityManager;
	private final GenreService genreService;

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

		return mediaMapper.toResponseDto(savedMedia);
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
