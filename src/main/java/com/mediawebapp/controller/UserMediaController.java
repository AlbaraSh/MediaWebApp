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

/**
 * REST entry point for the current user's personal media list.
 * <p>
 * Resolves identity via {@link CurrentUserProvider} and passes {@code userId}
 * into the service. Clients never send a user id.
 */
@RestController
@RequestMapping("/api/user-media")
@RequiredArgsConstructor
public class UserMediaController {

	private final UserMediaService userMediaService;
	private final CurrentUserProvider currentUserProvider;

	/**
	 * Upserts a list entry for the current user.
	 *
	 * @return {@code 201} when created, {@code 200} when updated
	 */
	@PostMapping
	public ResponseEntity<UserMediaResponseDTO> upsert(
			@Valid @RequestBody UserMediaRequestDTO requestDTO) {
		UUID userId = currentUserProvider.getCurrentUserId();
		UserMediaUpsertResult result = userMediaService.upsert(userId, requestDTO);
		HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
		return ResponseEntity.status(status).body(result.body());
	}

	/**
	 * Lists the current user's media as a page. Default status is COMPLETED.
	 * Breaking: response is a page envelope with counts, not a JSON array.
	 */
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

	/**
	 * Distinct genre names on the current user's shelf (all statuses).
	 */
	@GetMapping("/genres")
	public ResponseEntity<List<String>> listShelfGenres() {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(userMediaService.listShelfGenres(userId));
	}

	/**
	 * Returns the current user's shelf row for the given catalog media id.
	 */
	@GetMapping("/{mediaId}")
	public ResponseEntity<UserMediaResponseDTO> getByMediaId(@PathVariable UUID mediaId) {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(userMediaService.getForUser(userId, mediaId));
	}

	/**
	 * Deletes the current user's list entry for the given catalog media id.
	 */
	@DeleteMapping("/{mediaId}")
	public ResponseEntity<Void> delete(@PathVariable UUID mediaId) {
		UUID userId = currentUserProvider.getCurrentUserId();
		userMediaService.deleteForUser(userId, mediaId);
		return ResponseEntity.noContent().build();
	}
}
