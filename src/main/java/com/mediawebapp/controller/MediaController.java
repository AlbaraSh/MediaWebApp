package com.mediawebapp.controller;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.service.MediaService;
import jakarta.validation.Valid;
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

/**
 * REST entry point for the media catalog.
 * <p>
 * Responsible only for HTTP concerns: routing, status codes, and binding
 * request/response bodies. All business logic and DTO mapping live in
 * {@link MediaService}. JPA entities are never returned from this class.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final MediaService mediaService;

	/**
	 * Creates a media item.
	 * <p>
	 * {@code @Valid} triggers Bean Validation on {@link MediaRequestDTO}; failures
	 * become HTTP 400 via {@code GlobalExceptionHandler}.
	 *
	 * @param requestDTO JSON body for the new media item
	 * @return {@code 201 Created} with the persisted media representation
	 */
	@PostMapping
	public ResponseEntity<MediaResponseDTO> createMedia(
			@Valid @RequestBody MediaRequestDTO requestDTO) {
		MediaResponseDTO createdMedia = mediaService.createMedia(requestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMedia);
	}

	/**
	 * Lists all media items in the catalog.
	 *
	 * @return {@code 200 OK} with a list of media response DTOs
	 */
	@GetMapping
	public ResponseEntity<List<MediaResponseDTO>> getAllMedia() {
		return ResponseEntity.ok(mediaService.getAllMedia());
	}

	/**
	 * Fetches one media item by id.
	 * <p>
	 * Missing ids surface as {@code ResourceNotFoundException} → HTTP 404.
	 *
	 * @param id media primary key
	 * @return {@code 200 OK} with the media response DTO
	 */
	@GetMapping("/{id}")
	public ResponseEntity<MediaResponseDTO> getMediaById(@PathVariable UUID id) {
		return ResponseEntity.ok(mediaService.getMediaById(id));
	}
}
