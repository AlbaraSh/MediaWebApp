package com.mediawebapp.controller;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.service.ExternalMediaService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for the media catalog.
 * <p>
 * Responsible only for HTTP concerns: routing, status codes, and binding
 * request/response bodies. Catalog CRUD lives in {@link MediaService};
 * search/fetch/import live in {@link ExternalMediaService}. JPA entities
 * are never returned from this class.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final MediaService mediaService;
	private final ExternalMediaService externalMediaService;

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
	 * Searches a single external provider selected by {@code type}.
	 *
	 * @param query free-text search
	 * @param type  {@code MOVIE}, {@code TV}, {@code ANIME}, or {@code GAME}
	 * @return up to 10 unified DTOs (never merged across providers)
	 */
	@GetMapping("/search")
	public ResponseEntity<List<ExternalMediaDTO>> search(
			@RequestParam String query,
			@RequestParam String type) {
		return ResponseEntity.ok(externalMediaService.search(query, type));
	}

	/**
	 * Fetches one external title by provider and external id. Does not persist.
	 * TMDB ids must be prefixed ({@code movie:550} / {@code tv:1396}).
	 */
	@GetMapping("/external/{provider}/{externalId:.+}")
	public ResponseEntity<ExternalMediaDTO> getByExternalId(
			@PathVariable String provider,
			@PathVariable String externalId) {
		return ResponseEntity.ok(externalMediaService.getByExternalId(provider, externalId));
	}

	/**
	 * Imports external media into the catalog.
	 *
	 * @return {@code 201} when created, {@code 200} when already imported
	 */
	@PostMapping("/import")
	public ResponseEntity<MediaResponseDTO> importMedia(
			@Valid @RequestBody ImportMediaRequestDTO requestDTO) {
		ImportMediaResult result = externalMediaService.importMedia(requestDTO);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.body());
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
