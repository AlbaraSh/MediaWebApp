package com.mediawebapp.controller;

import com.mediawebapp.dto.LibraryPageResponse;
import com.mediawebapp.dto.Pagination;
import com.mediawebapp.dto.UserMediaRequestDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.dto.UserMediaUpsertResult;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.service.UserMediaService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's shelf. The user id comes from the JWT, never from the client. */
@RestController
@RequestMapping("/api/user-media")
@RequiredArgsConstructor
public class UserMediaController {

	private final UserMediaService userMediaService;
	private final CurrentUserProvider currentUserProvider;

	/** {@code 201} when the shelf row is new, {@code 200} when it is updated. */
	@PostMapping
	public ResponseEntity<UserMediaResponseDTO> upsert(
			@Valid @RequestBody UserMediaRequestDTO requestDTO) {
		UUID userId = currentUserProvider.getCurrentUserId();
		UserMediaUpsertResult result = userMediaService.upsert(userId, requestDTO);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.body());
	}

	/** Defaults to {@code COMPLETED}. The body includes status counts for the shelf tabs. */
	@GetMapping
	public ResponseEntity<LibraryPageResponse> getUserMedia(
			@RequestParam(required = false) UserMediaStatus status,
			@RequestParam(required = false) String type,
			@RequestParam(required = false) String genre,
			@RequestParam(required = false) Integer minRating,
			@RequestParam(required = false) Integer maxRating,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String sort,
			@RequestParam(required = false) String direction,
			@RequestParam(defaultValue = "" + Pagination.DEFAULT_PAGE) int page,
			@RequestParam(defaultValue = "" + Pagination.DEFAULT_SIZE) int size) {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(userMediaService.listForUser(
				userId, status, type, genre, minRating, maxRating, q, sort, direction, page, size));
	}

	/** Genres across every status, so the filter stays populated when a tab is empty. */
	@GetMapping("/genres")
	public ResponseEntity<List<String>> listShelfGenres() {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(userMediaService.listShelfGenres(userId));
	}

	@GetMapping("/{mediaId}")
	public ResponseEntity<UserMediaResponseDTO> getByMediaId(@PathVariable UUID mediaId) {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(userMediaService.getForUser(userId, mediaId));
	}

	@DeleteMapping("/{mediaId}")
	public ResponseEntity<Void> delete(@PathVariable UUID mediaId) {
		UUID userId = currentUserProvider.getCurrentUserId();
		userMediaService.deleteForUser(userId, mediaId);
		return ResponseEntity.noContent().build();
	}
}
