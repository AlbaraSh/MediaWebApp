package com.mediawebapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.RecommendationItemDTO;
import com.mediawebapp.dto.RecommendationResponseDTO;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.RecommendationService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RecommendationController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class RecommendationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RecommendationService recommendationService;

	@MockitoBean
	private CurrentUserProvider currentUserProvider;

	private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");

	@BeforeEach
	void setUp() {
		when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
	}

	@Test
	void similar_returnsGroupedPayload() throws Exception {
		when(recommendationService.findSimilar(mediaId)).thenReturn(sampleResponse());

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", mediaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies[0].mediaId").value(mediaId.toString()))
				.andExpect(jsonPath("$.movies[0].title").value("The Matrix"))
				.andExpect(jsonPath("$.movies[0].mediaType").value("Movie"))
				.andExpect(jsonPath("$.tvShows").isArray())
				.andExpect(jsonPath("$.anime").isArray())
				.andExpect(jsonPath("$.games").isArray());
	}

	@Test
	void similar_returns404WhenNotRecommendable() throws Exception {
		when(recommendationService.findSimilar(mediaId))
				.thenThrow(new ResourceNotFoundException("Media is not yet recommendable: " + mediaId));

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", mediaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media is not yet recommendable: " + mediaId))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void user_returns400WhenTypeInvalid() throws Exception {
		when(recommendationService.recommendForUser(userId, "BOOK"))
				.thenThrow(new BadRequestException("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"));

		mockMvc.perform(get("/api/recommendations/user").param("type", "BOOK"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void user_returns400WhenTypeMissing() throws Exception {
		mockMvc.perform(get("/api/recommendations/user"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Required parameter 'type' is missing"))
				.andExpect(jsonPath("$.status").value(400));

		verify(recommendationService, never()).recommendForUser(any(), any());
	}

	@Test
	void user_passesCurrentUserAndType() throws Exception {
		when(recommendationService.recommendForUser(userId, "TV"))
				.thenReturn(RecommendationResponseDTO.empty());

		mockMvc.perform(get("/api/recommendations/user").param("type", "TV"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies").isArray());

		verify(recommendationService).recommendForUser(eq(userId), eq("TV"));
	}

	private RecommendationResponseDTO sampleResponse() {
		RecommendationItemDTO movie = new RecommendationItemDTO(
				mediaId, "The Matrix", "Movie", (short) 1999, 8.7, 18500);
		return new RecommendationResponseDTO(List.of(movie), List.of(), List.of(), List.of());
	}
}
