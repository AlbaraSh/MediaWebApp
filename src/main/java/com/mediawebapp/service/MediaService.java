package com.mediawebapp.service;

import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import jakarta.persistence.EntityManager;
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
	private final EntityManager entityManager;

	@Transactional
	public Media createMedia(Media media) {
		UUID mediaTypeId = media.getMediaType().getId();
		MediaType mediaType = mediaTypeRepository.findById(mediaTypeId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media type not found with id: " + mediaTypeId));

		media.setMediaType(mediaType);
		Media saved = mediaRepository.saveAndFlush(media);
		entityManager.refresh(saved);
		return saved;
	}

	@Transactional(readOnly = true)
	public List<Media> getAllMedia() {
		return mediaRepository.findAllWithMediaType();
	}

	@Transactional(readOnly = true)
	public Media getMediaById(UUID id) {
		return mediaRepository.findByIdWithMediaType(id)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media not found with id: " + id));
	}
}
