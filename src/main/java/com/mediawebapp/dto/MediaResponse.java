package com.mediawebapp.dto;

import com.mediawebapp.entity.Media;
import java.time.Instant;
import java.util.UUID;

public record MediaResponse(
		UUID id,
		String title,
		String description,
		Short releaseYear,
		UUID mediaTypeId,
		String mediaTypeName,
		Instant createdAt,
		Instant updatedAt
) {

	public static MediaResponse from(Media media) {
		return new MediaResponse(
				media.getId(),
				media.getTitle(),
				media.getDescription(),
				media.getReleaseYear(),
				media.getMediaType().getId(),
				media.getMediaType().getName(),
				media.getCreatedAt(),
				media.getUpdatedAt()
		);
	}
}
