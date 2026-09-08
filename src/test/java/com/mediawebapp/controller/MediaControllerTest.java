package com.mediawebapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.service.MediaService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MediaController.class)
@Import(GlobalExceptionHandler.class)
class MediaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MediaService mediaService;

	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");

	/** POST /api/media with a valid body returns 201 and the created media JSON. */
	@Test
	void createMedia_returns201AndResponseBody() throws Exception {
		when(mediaService.createMedia(any())).thenReturn(sampleResponse("The Matrix", (short) 1999));

		String requestBody = """
				{
				  "title": "The Matrix",
				  "description": "A computer hacker learns about reality.",
				  "releaseYear": 1999,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(mediaId.toString()))
				.andExpect(jsonPath("$.title").value("The Matrix"))
				.andExpect(jsonPath("$.description").value("A computer hacker learns about reality."))
				.andExpect(jsonPath("$.releaseYear").value(1999))
				.andExpect(jsonPath("$.mediaType.id").value(mediaTypeId.toString()))
				.andExpect(jsonPath("$.mediaType.name").value("Movie"))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists())
				.andExpect(jsonPath("$.mediaTypeId").doesNotExist());
	}

	/** GET /api/media returns 200 and a JSON array of media items. */
	@Test
	void getAllMedia_returns200AndList() throws Exception {
		when(mediaService.getAllMedia()).thenReturn(List.of(
				sampleResponse("The Matrix", (short) 1999)
		));

		mockMvc.perform(get("/api/media"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0].id").value(mediaId.toString()))
				.andExpect(jsonPath("$[0].title").value("The Matrix"))
				.andExpect(jsonPath("$[0].mediaType.name").value("Movie"));
	}

	/** GET /api/media/{id} returns 200 and the media JSON when the id exists. */
	@Test
	void getMediaById_returns200() throws Exception {
		when(mediaService.getMediaById(mediaId)).thenReturn(sampleResponse("The Matrix", (short) 1999));

		mockMvc.perform(get("/api/media/{id}", mediaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mediaId.toString()))
				.andExpect(jsonPath("$.title").value("The Matrix"))
				.andExpect(jsonPath("$.mediaType.id").value(mediaTypeId.toString()));
	}

	/** GET /api/media/{id} returns 404 with the API error body when the media is missing. */
	@Test
	void getMediaById_returns404WhenNotFound() throws Exception {
		when(mediaService.getMediaById(mediaId))
				.thenThrow(new ResourceNotFoundException("Media not found with id: " + mediaId));

		mockMvc.perform(get("/api/media/{id}", mediaId))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media not found with id: " + mediaId))
				.andExpect(jsonPath("$.status").value(404));
	}

	/** POST /api/media with a blank title returns 400 validation error on title. */
	@Test
	void createMedia_returns400WhenTitleBlank() throws Exception {
		String requestBody = """
				{
				  "title": "",
				  "description": "Missing title",
				  "releaseYear": 1999,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.title").exists());
	}

	/** POST /api/media with releaseYear below the allowed range returns 400 on releaseYear. */
	@Test
	void createMedia_returns400WhenReleaseYearInvalid() throws Exception {
		String requestBody = """
				{
				  "title": "Ancient Film",
				  "description": "Too old",
				  "releaseYear": 1700,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.releaseYear").exists());
	}

	/** POST /api/media without mediaTypeId returns 400 validation error on mediaTypeId. */
	@Test
	void createMedia_returns400WhenMediaTypeIdMissing() throws Exception {
		String requestBody = """
				{
				  "title": "The Matrix",
				  "description": "No media type",
				  "releaseYear": 1999
				}
				""";

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.details.mediaTypeId").exists());
	}

	private MediaResponseDTO sampleResponse(String title, Short releaseYear) {
		return new MediaResponseDTO(
				mediaId,
				title,
				"A computer hacker learns about reality.",
				releaseYear,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-09-07T09:31:11.874953Z"),
				Instant.parse("2026-09-07T09:31:11.874953Z")
		);
	}
}
