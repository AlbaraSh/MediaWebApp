package com.mediawebapp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.ExternalMediaService;
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
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class MediaExternalControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MediaService mediaService;

	@MockitoBean
	private ExternalMediaService externalMediaService;

	private final UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");

	@Test
	void search_validType_returnsArray() throws Exception {
		when(externalMediaService.search("matrix", "MOVIE")).thenReturn(List.of(
				new ExternalMediaDTO("The Matrix", "A computer hacker.", (short) 1999, "Movie", "TMDB", "movie:603", List.of(), null, null)
		));

		mockMvc.perform(get("/api/media/search")
						.param("query", "matrix")
						.param("type", "MOVIE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("The Matrix"))
				.andExpect(jsonPath("$[0].provider").value("TMDB"))
				.andExpect(jsonPath("$[0].externalId").value("movie:603"));
	}

	@Test
	void search_missingType_returns400() throws Exception {
		mockMvc.perform(get("/api/media/search").param("query", "matrix"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Required parameter 'type' is missing"))
				.andExpect(jsonPath("$.status").value(400));

		verify(externalMediaService, never()).search(any(), any());
	}

	@Test
	void search_invalidType_returns400() throws Exception {
		when(externalMediaService.search("matrix", "BOOK"))
				.thenThrow(new BadRequestException("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"));

		mockMvc.perform(get("/api/media/search")
						.param("query", "matrix")
						.param("type", "BOOK"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void search_providerFailure_returns503() throws Exception {
		when(externalMediaService.search("matrix", "MOVIE"))
				.thenThrow(new ExternalProviderException("TMDB provider is unavailable"));

		mockMvc.perform(get("/api/media/search")
						.param("query", "matrix")
						.param("type", "MOVIE"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error").value("TMDB provider is unavailable"))
				.andExpect(jsonPath("$.status").value(503));
	}

	@Test
	void getByExternalId_success() throws Exception {
		when(externalMediaService.getByExternalId("tmdb", "movie:550")).thenReturn(
				new ExternalMediaDTO("Fight Club", "Overview", (short) 1999, "Movie", "TMDB", "movie:550", List.of(), null, null));

		mockMvc.perform(get("/api/media/external/{provider}/{externalId}", "tmdb", "movie:550"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Fight Club"))
				.andExpect(jsonPath("$.externalId").value("movie:550"))
				.andExpect(jsonPath("$.provider").value("TMDB"));
	}

	@Test
	void getByExternalId_invalidProvider_returns400() throws Exception {
		when(externalMediaService.getByExternalId("imdb", "tt123"))
				.thenThrow(new BadRequestException("Invalid provider. Must be one of: tmdb, jikan, rawg"));

		mockMvc.perform(get("/api/media/external/{provider}/{externalId}", "imdb", "tt123"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void getByExternalId_notFound_returns404() throws Exception {
		when(externalMediaService.getByExternalId("jikan", "0"))
				.thenThrow(new ResourceNotFoundException("Jikan anime not found with id: 0"));

		mockMvc.perform(get("/api/media/external/{provider}/{externalId}", "jikan", "0"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void import_newMedia_returns201() throws Exception {
		when(externalMediaService.importMedia(any()))
				.thenReturn(new ImportMediaResult(sampleResponse(), true));

		mockMvc.perform(post("/api/media/import")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "tmdb",
								  "externalId": "movie:550"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(mediaId.toString()))
				.andExpect(jsonPath("$.title").value("Fight Club"));
	}

	@Test
	void import_alreadyImported_returns200() throws Exception {
		when(externalMediaService.importMedia(any()))
				.thenReturn(new ImportMediaResult(sampleResponse(), false));

		mockMvc.perform(post("/api/media/import")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "tmdb",
								  "externalId": "movie:550"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mediaId.toString()));
	}

	@Test
	void import_uppercaseProvider_returns400() throws Exception {
		mockMvc.perform(post("/api/media/import")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "TMDB",
								  "externalId": "movie:550"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Validation failed"))
				.andExpect(jsonPath("$.details.provider").exists());

		verify(externalMediaService, never()).importMedia(any());
	}

	private MediaResponseDTO sampleResponse() {
		return new MediaResponseDTO(
				mediaId,
				"Fight Club",
				"Overview",
				(short) 1999,
				List.of(),
				null,
				null,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-01T00:00:00Z")
		);
	}
}
