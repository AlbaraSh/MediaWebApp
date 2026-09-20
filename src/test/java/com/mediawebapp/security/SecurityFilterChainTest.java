package com.mediawebapp.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.controller.GenreController;
import com.mediawebapp.controller.MediaController;
import com.mediawebapp.controller.RecommendationController;
import com.mediawebapp.controller.UserMediaController;
import com.mediawebapp.dto.LibraryPageResponse;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.dto.MediaTypeDTO;
import com.mediawebapp.dto.PageResponse;
import com.mediawebapp.dto.RecommendationResponseDTO;
import com.mediawebapp.dto.UserMediaStatusCounts;
import com.mediawebapp.entity.User;
import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.repository.UserRepository;
import com.mediawebapp.service.ExternalMediaService;
import com.mediawebapp.service.GenreService;
import com.mediawebapp.service.MediaService;
import com.mediawebapp.service.RecommendationService;
import com.mediawebapp.service.UserMediaService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Exercises the real JWT filter chain. Do not import {@link TestSecurityConfig}
 * here — that bypass would make the 401 assertion pass for the wrong reason.
 */
@ActiveProfiles("test")
@WebMvcTest(controllers = {
		UserMediaController.class,
		MediaController.class,
		RecommendationController.class,
		GenreController.class
})
@Import({
		SecurityConfig.class,
		JwtService.class,
		JwtCurrentUserProvider.class,
		JsonAuthenticationEntryPoint.class,
		JsonAccessDeniedHandler.class,
		GlobalExceptionHandler.class
})
class SecurityFilterChainTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private UserMediaService userMediaService;

	@MockitoBean
	private MediaService mediaService;

	@MockitoBean
	private ExternalMediaService externalMediaService;

	@MockitoBean
	private RecommendationService recommendationService;

	@MockitoBean
	private GenreService genreService;

	@MockitoBean
	private UserRepository userRepository;

	private final UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private final UUID catalogAdminId = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private final UUID mediaTypeId = UUID.fromString("5f73d14b-4df1-499f-8fa9-ba5a2e0c4421");

	@BeforeEach
	void stubExistingUsers() {
		stubUser(userId, "alice@example.com", 0);
		stubUser(catalogAdminId, "dev@mediawebapp.local", 0);
	}

	private void stubUser(UUID id, String email, int tokenVersion) {
		User user = new User();
		user.setId(id);
		user.setEmail(email);
		user.setTokenVersion(tokenVersion);
		when(userRepository.findById(id)).thenReturn(Optional.of(user));
	}

	@Test
	void protectedEndpoint_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/user-media"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void protectedEndpoint_withValidToken_succeeds() throws Exception {
		when(userMediaService.listForUser(eq(userId), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(LibraryPageResponse.of(
						PageResponse.of(List.of(), 0, 20, 0),
						UserMediaStatusCounts.empty()));
		String token = jwtService.generateToken(userId, "alice@example.com");

		mockMvc.perform(get("/api/user-media")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.counts.completed").value(0));
	}

	@Test
	void mediaWrite_withoutToken_returns401() throws Exception {
		String body = """
				{
				  "title": "The Matrix",
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void mediaWrite_withNonAdminToken_returns403() throws Exception {
		String token = jwtService.generateToken(userId, "alice@example.com");

		String body = """
				{
				  "title": "The Matrix",
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Access denied"))
				.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	void mediaWrite_withCatalogAdminToken_succeeds() throws Exception {
		UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
		when(mediaService.createMedia(any())).thenReturn(new MediaResponseDTO(
				mediaId,
				"The Matrix",
				null,
				null,
				List.of(),
				null,
				null,
				new MediaTypeDTO(mediaTypeId, "Movie"),
				Instant.parse("2026-09-13T08:00:00Z"),
				Instant.parse("2026-09-13T08:00:00Z"),
				null
		));
		String token = jwtService.generateToken(catalogAdminId, "dev@mediawebapp.local");

		String body = """
				{
				  "title": "The Matrix",
				  "mediaTypeId": "%s"
				}
				""".formatted(mediaTypeId);

		mockMvc.perform(post("/api/media")
						.header("Authorization", "Bearer " + token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("The Matrix"));
	}

	@Test
	void mediaRead_withoutToken_succeeds() throws Exception {
		when(mediaService.discover(any(), any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
				.thenReturn(PageResponse.of(List.of(), 0, 20, 0));

		mockMvc.perform(get("/api/media"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.page").value(0));
	}

	@Test
	void genres_withoutToken_succeeds() throws Exception {
		when(genreService.listDistinctCatalogNames()).thenReturn(List.of("Action"));

		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0]").value("Action"));
	}

	@Test
	void mediaSearch_withoutToken_succeeds() throws Exception {
		when(externalMediaService.search("matrix", "MOVIE")).thenReturn(List.of());

		mockMvc.perform(get("/api/media/search")
						.param("query", "matrix")
						.param("type", "MOVIE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void mediaImport_withoutToken_returns401() throws Exception {
		mockMvc.perform(post("/api/media/import")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "tmdb",
								  "externalId": "movie:550"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void protectedEndpoint_withDeletedUserToken_returns401() throws Exception {
		when(userRepository.findById(userId)).thenReturn(Optional.empty());
		String token = jwtService.generateToken(userId, "alice@example.com");

		mockMvc.perform(get("/api/user-media")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired JWT"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void protectedEndpoint_withStaleTokenVersion_returns401() throws Exception {
		stubUser(userId, "alice@example.com", 1);
		String token = jwtService.generateToken(userId, "alice@example.com", 0);

		mockMvc.perform(get("/api/user-media")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired JWT"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void protectedEndpoint_withInvalidToken_returns401() throws Exception {
		mockMvc.perform(get("/api/user-media")
						.header("Authorization", "Bearer not-a-real-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired JWT"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void similarRecommendations_withoutToken_succeeds() throws Exception {
		UUID mediaId = UUID.fromString("378374f4-700b-422a-80f8-a3a802925fb7");
		when(recommendationService.findSimilar(mediaId)).thenReturn(RecommendationResponseDTO.empty());

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", mediaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies").isArray());
	}

	@Test
	void userRecommendations_withoutToken_returns401() throws Exception {
		mockMvc.perform(get("/api/recommendations/user").param("type", "MOVIE"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void userRecommendations_withValidToken_succeeds() throws Exception {
		when(recommendationService.recommendForUser(userId, "MOVIE"))
				.thenReturn(RecommendationResponseDTO.empty());
		String token = jwtService.generateToken(userId, "alice@example.com");

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "MOVIE")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies").isArray());
	}
}
