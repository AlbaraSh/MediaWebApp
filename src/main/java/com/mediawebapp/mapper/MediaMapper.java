package com.mediawebapp.mapper;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import org.springframework.stereotype.Component;

/**
 * Central place for converting between media API DTOs and JPA entities.
 * <p>
 * Keeping all mapping here prevents duplicated conversion logic in controllers
 * and services, and makes it easier to evolve the API contract without
 * scattering field assignments across the codebase.
 */
@Component
public class MediaMapper {

	/**
	 * Builds a transient {@link Media} from a create request.
	 * <p>
	 * Only the media-type id is attached here; the service layer resolves the
	 * full {@link MediaType} entity before persisting so the foreign key is
	 * validated against the database.
	 *
	 * @param request validated create payload from the client
	 * @return unsaved media entity ready for the service layer
	 */
	public Media toEntity(MediaRequestDTO request) {
		Media media = new Media();
		media.setTitle(request.title());
		media.setDescription(request.description());
		media.setReleaseYear(request.releaseYear());

		MediaType mediaTypeReference = new MediaType();
		mediaTypeReference.setId(request.mediaTypeId());
		media.setMediaType(mediaTypeReference);

		return media;
	}

	/**
	 * Converts a persisted {@link Media} (with media type loaded) into an API response.
	 *
	 * @param media managed entity including its {@code mediaType} association
	 * @return response DTO safe to serialize over HTTP
	 */
	public MediaResponseDTO toResponseDto(Media media) {
		return new MediaResponseDTO(
				media.getId(),
				media.getTitle(),
				media.getDescription(),
				media.getReleaseYear(),
				toMediaTypeDto(media.getMediaType()),
				media.getCreatedAt(),
				media.getUpdatedAt()
		);
	}

	/**
	 * Maps a {@link MediaType} entity to its nested DTO form.
	 *
	 * @param mediaType loaded media type entity
	 * @return nested media-type DTO for API responses
	 */
	public MediaTypeDTO toMediaTypeDto(MediaType mediaType) {
		return new MediaTypeDTO(mediaType.getId(), mediaType.getName());
	}
}
