package com.mediawebapp.controller;

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
	 * Lists the current user's media entries, optionally filtered by status.
	 *
	 * @param status optional {@link UserMediaStatus} query value
	 */
	@GetMapping
	public ResponseEntity<List<UserMediaResponseDTO>> getUserMedia(
			@RequestParam(required = false) UserMediaStatus status) {
		UUID userId = currentUserProvider.getCurrentUserId();
		List<UserMediaResponseDTO> responses = status == null
				? userMediaService.getAllForUser(userId)
				: userMediaService.getAllForUserByStatus(userId, status);
		return ResponseEntity.ok(responses);
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
