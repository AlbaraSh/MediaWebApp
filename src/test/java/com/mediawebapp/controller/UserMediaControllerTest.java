package com.mediawebapp.controller;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.dto.UserMediaResponseDTO;
import com.mediawebapp.dto.UserMediaUpsertResult;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.security.CurrentUserProvider;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.UserMediaService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserMediaController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class UserMediaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserMediaService userMediaService;

	@MockitoBean
	private CurrentUserProvider currentUserProvider;

	private final UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");

	@BeforeEach
	void setUp() {
		when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
	}

	/** POST /api/user-media returns 201 and body when the service creates a new entry. */
	@Test
	void upsert_returns201WhenCreated() throws Exception {
		when(userMediaService.upsert(eq(userId), any()))
				.thenReturn(new UserMediaUpsertResult(sampleResponse(), true));

		String body = """
				{
				  "mediaId": "%s",
				  "status": "WATCHING",
				  "rating": 8,
				  "review": "Great film"
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("WATCHING"))
				.andExpect(jsonPath("$.rating").value(8))
				.andExpect(jsonPath("$.review").value("Great film"))
				.andExpect(jsonPath("$.mediaId").value(mediaId.toString()))
				.andExpect(jsonPath("$.title").value("The Matrix"))
				.andExpect(jsonPath("$.mediaType.id").value(mediaTypeId.toString()))
				.andExpect(jsonPath("$.mediaType.name").value("Movie"))
				.andExpect(jsonPath("$.userId").doesNotExist());

		verify(userMediaService).upsert(eq(userId), any());
	}

	/** POST /api/user-media returns 200 when the service updates an existing entry. */
	@Test
	void upsert_returns200WhenUpdated() throws Exception {
		when(userMediaService.upsert(eq(userId), any()))
				.thenReturn(new UserMediaUpsertResult(sampleResponse(), false));

		String body = """
				{
				  "mediaId": "%s",
				  "status": "COMPLETED",
				  "rating": 9
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mediaId").value(mediaId.toString()));
	}

	/** POST with only required fields (mediaId + status) is accepted; rating/review optional. */
	@Test
	void upsert_returns201WhenRatingAndReviewOmitted() throws Exception {
		UserMediaResponseDTO response = new UserMediaResponseDTO(
				UserMediaStatus.PLANNED,
				null,
				null,
				Instant.parse("2026-09-08T10:00:00Z"),
				Instant.parse("2026-09-08T10:00:00Z"),
				mediaId,
				"The Matrix",
				(short) 1999,
				new MediaTypeDTO(mediaTypeId, "Movie")
		);
		when(userMediaService.upsert(eq(userId), any()))
				.thenReturn(new UserMediaUpsertResult(response, true));

		String body = """
				{
				  "mediaId": "%s",
				  "status": "PLANNED"
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.rating").value(nullValue()))
				.andExpect(jsonPath("$.review").value(nullValue()));
	}

	/** GET /api/user-media returns 200 and the current user's full list. */
	@Test
	void getUserMedia_returns200AndList() throws Exception {
		when(userMediaService.getAllForUser(userId)).thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/user-media"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].mediaId").value(mediaId.toString()))
				.andExpect(jsonPath("$[0].status").value("WATCHING"));

		verify(userMediaService).getAllForUser(userId);
		verify(userMediaService, never()).getAllForUserByStatus(any(), any());
	}

	/** GET /api/user-media returns 200 with an empty array when the user has no entries. */
	@Test
	void getUserMedia_returns200AndEmptyList() throws Exception {
		when(userMediaService.getAllForUser(userId)).thenReturn(List.of());

		mockMvc.perform(get("/api/user-media"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
	}

	/** GET /api/user-media?status=COMPLETED filters via getAllForUserByStatus. */
	@Test
	void getUserMedia_returnsFilteredByStatus() throws Exception {
		when(userMediaService.getAllForUserByStatus(userId, UserMediaStatus.COMPLETED))
				.thenReturn(List.of(sampleResponse()));

		mockMvc.perform(get("/api/user-media").param("status", "COMPLETED"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].mediaId").value(mediaId.toString()));

		verify(userMediaService).getAllForUserByStatus(userId, UserMediaStatus.COMPLETED);
		verify(userMediaService, never()).getAllForUser(any());
	}

	/** GET with an unknown status query value is rejected (enum binding failure). */
	@Test
	void getUserMedia_returns400WhenStatusQueryInvalid() throws Exception {
		mockMvc.perform(get("/api/user-media").param("status", "BINGEING"))
				.andExpect(status().isBadRequest());

		verify(userMediaService, never()).getAllForUser(any());
		verify(userMediaService, never()).getAllForUserByStatus(any(), any());
	}

	/** DELETE /api/user-media/{mediaId} returns 204 when the entry is removed. */
	@Test
	void delete_returns204WhenSuccessful() throws Exception {
		mockMvc.perform(delete("/api/user-media/{mediaId}", mediaId))
				.andExpect(status().isNoContent());

		verify(userMediaService).deleteForUser(userId, mediaId);
	}

	/** DELETE returns 404 with the API error body when the entry is missing. */
	@Test
	void delete_returns404WhenNotFound() throws Exception {
		doThrow(new ResourceNotFoundException("User media entry not found for media id: " + mediaId))
				.when(userMediaService).deleteForUser(userId, mediaId);

		mockMvc.perform(delete("/api/user-media/{mediaId}", mediaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("User media entry not found for media id: " + mediaId))
				.andExpect(jsonPath("$.status").value(404));
	}

	/** POST with rating above 10 returns 400 validation error on rating. */
	@Test
	void upsert_returns400WhenRatingTooHigh() throws Exception {
		String body = """
				{
				  "mediaId": "%s",
				  "status": "WATCHING",
				  "rating": 11
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.rating").exists());

		verify(userMediaService, never()).upsert(any(), any());
	}

	/** POST with rating below 1 returns 400 validation error on rating. */
	@Test
	void upsert_returns400WhenRatingTooLow() throws Exception {
		String body = """
				{
				  "mediaId": "%s",
				  "status": "WATCHING",
				  "rating": 0
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.rating").exists());

		verify(userMediaService, never()).upsert(any(), any());
	}

	/** POST with a status not in the allowed set returns 400 on status. */
	@Test
	void upsert_returns400WhenStatusInvalid() throws Exception {
		String body = """
				{
				  "mediaId": "%s",
				  "status": "BINGEING",
				  "rating": 5
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.status").exists());

		verify(userMediaService, never()).upsert(any(), any());
	}

	/** POST without status returns 400 validation error on status. */
	@Test
	void upsert_returns400WhenStatusMissing() throws Exception {
		String body = """
				{
				  "mediaId": "%s",
				  "rating": 5
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.status").exists());

		verify(userMediaService, never()).upsert(any(), any());
	}

	/** POST without mediaId returns 400 validation error on mediaId. */
	@Test
	void upsert_returns400WhenMediaIdMissing() throws Exception {
		String body = """
				{
				  "status": "PLANNED"
				}
				""";

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.mediaId").exists());

		verify(userMediaService, never()).upsert(any(), any());
	}

	/** POST returns 404 when the referenced catalog media does not exist. */
	@Test
	void upsert_returns404WhenMediaMissing() throws Exception {
		when(userMediaService.upsert(eq(userId), any()))
				.thenThrow(new ResourceNotFoundException("Media not found with id: " + mediaId));

		String body = """
				{
				  "mediaId": "%s",
				  "status": "PLANNED"
				}
				""".formatted(mediaId);

		mockMvc.perform(post("/api/user-media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media not found with id: " + mediaId))
				.andExpect(jsonPath("$.status").value(404));
	}

	private UserMediaResponseDTO sampleResponse() {
		return new UserMediaResponseDTO(
				UserMediaStatus.WATCHING,
				8,
				"Great film",
				Instant.parse("2026-09-08T10:00:00Z"),
				Instant.parse("2026-09-08T10:00:00Z"),
				mediaId,
				"The Matrix",
				(short) 1999,
				new MediaTypeDTO(mediaTypeId, "Movie")
		);
	}
}
