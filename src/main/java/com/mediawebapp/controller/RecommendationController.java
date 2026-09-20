package com.mediawebapp.controller;

import com.mediawebapp.dto.RecommendationResponseDTO;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.service.RecommendationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Similar-media discovery and personalized recommendations.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

	private final RecommendationService recommendationService;
	private final CurrentUserProvider currentUserProvider;

	/**
	 * Top 20 catalog items most similar to {@code mediaId}, grouped by type.
	 */
	@GetMapping("/similar/{mediaId}")
	public ResponseEntity<RecommendationResponseDTO> findSimilar(@PathVariable UUID mediaId) {
		return ResponseEntity.ok(recommendationService.findSimilar(mediaId));
	}

	/**
	 * Personalized recommendations for the current user. {@code type} is required
	 * and receives more results than the other three types (20 / 5 / 5 / 5).
	 */
	@GetMapping("/user")
	public ResponseEntity<RecommendationResponseDTO> recommendForUser(@RequestParam String type) {
		UUID userId = currentUserProvider.getCurrentUserId();
		return ResponseEntity.ok(recommendationService.recommendForUser(userId, type));
	}
}
