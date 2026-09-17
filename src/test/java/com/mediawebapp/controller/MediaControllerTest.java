package com.mediawebapp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.ExternalMediaService;
import com.mediawebapp.service.MediaService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MediaController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class MediaControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MediaService mediaService;

	@MockitoBean
	private ExternalMediaService externalMediaService;

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
				.andExpect(jsonPath("$.genres").isArray())
				.andExpect(jsonPath("$.rating").value(nullValue()))
				.andExpect(jsonPath("$.ratingCount").value(nullValue()))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists())
				.andExpect(jsonPath("$.mediaTypeId").doesNotExist())
				.andExpect(jsonPath("$.ratingLastUpdatedAt").doesNotExist());
	}

	/** POST /api/media accepts optional genres, rating, and ratingCount used by the import-parity create path. */
	@Test
	void createMedia_acceptsOptionalGenresAndRating() throws Exception {
		when(mediaService.createMedia(any())).thenReturn(new MediaResponseDTO(
				mediaId,
				"The Matrix",
				"A computer hacker learns about reality.",
				(short) 1999,
				List.of("Action", "Sci-Fi"),
				8.7,
				18500,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-09-07T09:31:11.874953Z"),
				Instant.parse("2026-09-07T09:31:11.874953Z")
		));

		String requestBody = """
				{
				  "title": "The Matrix",
				  "description": "A computer hacker learns about reality.",
				  "releaseYear": 1999,
				  "mediaTypeId": "%s",
				  "genres": ["action", "Sci-Fi"],
				  "rating": 8.7,
				  "ratingCount": 18500
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.genres[0]").value("Action"))
				.andExpect(jsonPath("$.genres[1]").value("Sci-Fi"))
				.andExpect(jsonPath("$.rating").value(8.7))
				.andExpect(jsonPath("$.ratingCount").value(18500));

		ArgumentCaptor<MediaRequestDTO> captor = ArgumentCaptor.forClass(MediaRequestDTO.class);
		verify(mediaService).createMedia(captor.capture());
		assertThat(captor.getValue().genres()).containsExactly("action", "Sci-Fi");
		assertThat(captor.getValue().rating()).isEqualTo(8.7);
		assertThat(captor.getValue().ratingCount()).isEqualTo(18500);
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

	/** GET /api/media returns 200 with an empty array when the catalog is empty. */
	@Test
	void getAllMedia_returns200AndEmptyList() throws Exception {
		when(mediaService.getAllMedia()).thenReturn(List.of());

		mockMvc.perform(get("/api/media"))
				.andExpect(status().isOk())
				.andExpect(content().json("[]"));
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

	/** GET /api/media/{id} with a non-UUID path value is rejected by argument binding. */
	@Test
	void getMediaById_returns400WhenIdNotUuid() throws Exception {
		mockMvc.perform(get("/api/media/{id}", "not-a-uuid"))
				.andExpect(status().isBadRequest());

		verify(mediaService, never()).getMediaById(any());
	}

	/** POST with optional description/releaseYear omitted returns 201. */
	@Test
	void createMedia_returns201WhenOptionalFieldsOmitted() throws Exception {
		MediaResponseDTO response = new MediaResponseDTO(
				mediaId,
				"Untitled",
				null,
				null,
				List.of(),
				null,
				null,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-09-07T09:31:11.874953Z"),
				Instant.parse("2026-09-07T09:31:11.874953Z")
		);
		when(mediaService.createMedia(any())).thenReturn(response);

		String requestBody = """
				{
				  "title": "Untitled",
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Untitled"))
				.andExpect(jsonPath("$.description").value(nullValue()))
				.andExpect(jsonPath("$.releaseYear").value(nullValue()));
	}

	/** POST returns 404 when the referenced media type does not exist. */
	@Test
	void createMedia_returns404WhenMediaTypeMissing() throws Exception {
		when(mediaService.createMedia(any()))
				.thenThrow(new ResourceNotFoundException("Media type not found with id: " + mediaTypeId));

		String requestBody = """
				{
				  "title": "The Matrix",
				  "releaseYear": 1999,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media type not found with id: " + mediaTypeId))
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

		verify(mediaService, never()).createMedia(any());
	}

	/** POST with whitespace-only title fails @NotBlank validation. */
	@Test
	void createMedia_returns400WhenTitleWhitespaceOnly() throws Exception {
		String requestBody = """
				{
				  "title": "   ",
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.title").exists());

		verify(mediaService, never()).createMedia(any());
	}

	/** POST without title returns 400 validation error on title. */
	@Test
	void createMedia_returns400WhenTitleMissing() throws Exception {
		String requestBody = """
				{
				  "description": "No title",
				  "releaseYear": 1999,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.title").exists());

		verify(mediaService, never()).createMedia(any());
	}

	/** POST with title longer than 500 characters returns 400 on title. */
	@Test
	void createMedia_returns400WhenTitleTooLong() throws Exception {
		String requestBody = """
				{
				  "title": "%s",
				  "mediaTypeId": "%s"
				}
				""".formatted("a".repeat(501), mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.title").exists());

		verify(mediaService, never()).createMedia(any());
	}

	/** POST /api/media with releaseYear below the allowed range returns 400 on releaseYear. */
	@Test
	void createMedia_returns400WhenReleaseYearTooLow() throws Exception {
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

		verify(mediaService, never()).createMedia(any());
	}

	/** POST with releaseYear above 2100 returns 400 on releaseYear. */
	@Test
	void createMedia_returns400WhenReleaseYearTooHigh() throws Exception {
		String requestBody = """
				{
				  "title": "Future Film",
				  "releaseYear": 2101,
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.releaseYear").exists());

		verify(mediaService, never()).createMedia(any());
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

		verify(mediaService, never()).createMedia(any());
	}

	private MediaResponseDTO sampleResponse(String title, Short releaseYear) {
		return new MediaResponseDTO(
				mediaId,
				title,
				"A computer hacker learns about reality.",
				releaseYear,
				List.of(),
				null,
				null,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-09-07T09:31:11.874953Z"),
				Instant.parse("2026-09-07T09:31:11.874953Z")
		);
	}
}
