package com.mediawebapp.controller;

import com.mediawebapp.dto.MediaRequest;
import com.mediawebapp.dto.MediaResponse;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.service.MediaService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final MediaService mediaService;

	@PostMapping
	public ResponseEntity<MediaResponse> createMedia(@RequestBody MediaRequest request) {
		Media media = toEntity(request);
		Media created = mediaService.createMedia(media);
		return ResponseEntity.status(HttpStatus.CREATED).body(MediaResponse.from(created));
	}

	@GetMapping
	public ResponseEntity<List<MediaResponse>> getAllMedia() {
		List<MediaResponse> responses = mediaService.getAllMedia().stream()
				.map(MediaResponse::from)
				.toList();
		return ResponseEntity.ok(responses);
	}

	@GetMapping("/{id}")
	public ResponseEntity<MediaResponse> getMediaById(@PathVariable UUID id) {
		Media media = mediaService.getMediaById(id);
		return ResponseEntity.ok(MediaResponse.from(media));
	}

	private Media toEntity(MediaRequest request) {
		Media media = new Media();
		media.setTitle(request.title());
		media.setDescription(request.description());
		media.setReleaseYear(request.releaseYear());

		MediaType mediaType = new MediaType();
		mediaType.setId(request.mediaTypeId());
		media.setMediaType(mediaType);

		return media;
	}
}
