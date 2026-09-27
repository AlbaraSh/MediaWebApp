package com.mediawebapp.controller;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.PageResponse;
import com.mediawebapp.dto.Pagination;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.service.ExternalMediaService;
import com.mediawebapp.service.MediaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;
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
 * Catalog HTTP API. Reads are public. Direct writes are admin-only;
 * search and import go through external providers.
 */
@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
public class MediaController {

	private final MediaService mediaService;
	private final ExternalMediaService externalMediaService;
	private final CurrentUserProvider currentUserProvider;

	@PostMapping
	public ResponseEntity<MediaResponseDTO> createMedia(
			@Valid @RequestBody MediaRequestDTO requestDTO) {
		MediaResponseDTO createdMedia = mediaService.createMedia(requestDTO);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMedia);
	}

	@GetMapping
	public ResponseEntity<PageResponse<MediaResponseDTO>> getAllMedia(
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String genre,
			@RequestParam(required = false) Integer year,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction,
			@RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size) {
		Optional<UUID> currentUserId = currentUserProvider.getCurrentUserIdIfPresent();
		return ResponseEntity.ok(mediaService.discover(
				currentUserId, type, genre, year, q, sort, direction, page, size));
	}

	/** One provider per {@code type}. Results are not merged across providers. */
	@GetMapping("/search")
	public ResponseEntity<List<ExternalMediaDTO>> search(
			@RequestParam String query,
			@RequestParam String type) {
		return ResponseEntity.ok(externalMediaService.search(query, type));
	}

	/** Does not persist. TMDB ids stay prefixed, for example {@code movie:550}. */
	@GetMapping("/external/{provider}/{externalId:.+}")
	public ResponseEntity<ExternalMediaDTO> getByExternalId(
			@PathVariable String provider,
			@PathVariable String externalId) {
		return ResponseEntity.ok(externalMediaService.getByExternalId(provider, externalId));
	}

	/** {@code 201} when the title is new, {@code 200} when that external id is already imported. */
	@PostMapping("/import")
	public ResponseEntity<MediaResponseDTO> importMedia(
			@Valid @RequestBody ImportMediaRequestDTO requestDTO) {
		ImportMediaResult result = externalMediaService.importMedia(requestDTO);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.body());
	}

	@GetMapping("/{id}")
	public ResponseEntity<MediaResponseDTO> getMediaById(@PathVariable UUID id) {
		return ResponseEntity.ok(mediaService.getMediaById(id));
	}
}
