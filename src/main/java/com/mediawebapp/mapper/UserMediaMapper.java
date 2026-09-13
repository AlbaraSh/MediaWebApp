package com.mediawebapp.mapper;

import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.UserMedia;
import org.springframework.stereotype.Component;

/**
 * Maps {@link UserMedia} entities (with media + mediaType loaded) to API DTOs.
 */
@Component
public class UserMediaMapper {

	/**
	 * @param userMedia persisted entry with {@code media.mediaType} initialized
	 * @return API response DTO
	 */
	public UserMediaResponseDTO toResponseDto(UserMedia userMedia) {
		Media media = userMedia.getMedia();
		return new UserMediaResponseDTO(
				userMedia.getStatus(),
				userMedia.getRating(),
				userMedia.getReview(),
				userMedia.getCreatedAt(),
				userMedia.getUpdatedAt(),
				media.getId(),
				media.getTitle(),
				media.getReleaseYear(),
				new MediaTypeDTO(media.getMediaType().getId(), media.getMediaType().getName())
		);
	}
}
