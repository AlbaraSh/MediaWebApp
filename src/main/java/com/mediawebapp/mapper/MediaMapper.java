package com.mediawebapp.mapper;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.entity.Genre;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class MediaMapper {

	public Media toEntity(MediaRequestDTO request) {
		Media media = new Media();
		media.setTitle(request.title());
		media.setDescription(request.description());
		media.setReleaseYear(request.releaseYear());
		media.setExternalRating(request.rating());
		media.setExternalRatingCount(request.ratingCount());
		media.setPosterUrl(request.posterUrl());

		MediaType mediaTypeReference = new MediaType();
		mediaTypeReference.setId(request.mediaTypeId());
		media.setMediaType(mediaTypeReference);

		return media;
	}

	public MediaResponseDTO toResponseDto(Media media) {
		return toResponseDto(media, null);
	}

	public MediaResponseDTO toResponseDto(Media media, Boolean inLibrary) {
		return new MediaResponseDTO(
				media.getId(),
				media.getTitle(),
				media.getDescription(),
				media.getReleaseYear(),
				toGenreNames(media),
				media.getExternalRating(),
				media.getExternalRatingCount(),
				toMediaTypeDto(media.getMediaType()),
				media.getCreatedAt(),
				media.getUpdatedAt(),
				inLibrary,
				media.getPosterUrl()
		);
	}

	public MediaTypeDTO toMediaTypeDto(MediaType mediaType) {
		return new MediaTypeDTO(mediaType.getId(), mediaType.getName());
	}

	private static List<String> toGenreNames(Media media) {
		if (media.getGenres() == null || media.getGenres().isEmpty()) {
			return List.of();
		}
		return media.getGenres().stream()
				.map(Genre::getName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();
	}
}
