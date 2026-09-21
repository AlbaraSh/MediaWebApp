package com.mediawebapp.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.config.TestContainerConfig;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.external.adapter.OpenAiEmbeddingAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.dto.tmdb.TmdbGenre;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.repository.EmbeddingVectorFormat;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.service.MediaService;
import com.mediawebapp.service.RecommendationScoring;
import com.mediawebapp.security.JwtService;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * End-to-end HTTP coverage of the main product flows against a real Postgres.
 * External HTTP (TMDB / OpenAI) is stubbed; catalog, library, auth, and recs are not.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
class ApplicationFlowIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private MediaTypeRepository mediaTypeRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private MediaService mediaService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private CacheManager cacheManager;

	@MockitoBean
	private OpenAiEmbeddingAdapter openAiEmbeddingAdapter;

	@MockitoBean
	private TmdbAdapter tmdbAdapter;

	private UUID movieTypeId;
	private UUID tvTypeId;

	@BeforeEach
	void loadTypes() {
		for (String name : CacheConfig.CACHE_NAMES) {
			Cache cache = cacheManager.getCache(name);
			if (cache != null) {
				cache.clear();
			}
		}
		movieTypeId = mediaTypeRepository.findByName("Movie").orElseThrow().getId();
		tvTypeId = mediaTypeRepository.findByName("TV Show").orElseThrow().getId();
	}

	@Test
	void auth_registerLoginThenProtectsLibrary() throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "alice@example.com",
								  "username": "alice",
								  "password": "secret123"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(content().string(""));

		String token = login("alice@example.com", "secret123");

		mockMvc.perform(get("/api/user-media").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.counts.completed").value(0));

		mockMvc.perform(get("/api/user-media"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		mockMvc.perform(post("/api/auth/logout").header("Authorization", token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/user-media").header("Authorization", token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid or expired JWT"));

		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));

		mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "alice@example.com",
								  "password": "wrong-password"
								}
								"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("Invalid email or password"));

		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "alice@example.com",
								  "username": "alice2",
								  "password": "secret123"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("Email is already registered"));
	}

	@Test
	void catalog_createThenPublicDiscoverAndGetById() throws Exception {
		String token = registerAndLogin("bob@example.com", "bob");

		mockMvc.perform(post("/api/media")
						.contentType(MediaType.APPLICATION_JSON)
						.content(createMovieBody("The Matrix", 1999, List.of("action"), 8.7, 18500)))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/media")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createMovieBody("The Matrix", 1999, List.of("action"), 8.7, 18500)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("Access denied"));

		String adminToken = "Bearer " + jwtService.generateToken(
				UUID.fromString("00000000-0000-0000-0000-000000000001"),
				"dev@mediawebapp.local");
		String created = mockMvc.perform(post("/api/media")
						.header("Authorization", adminToken)
						.contentType(MediaType.APPLICATION_JSON)
						.content(createMovieBody("The Matrix", 1999, List.of("action"), 8.7, 18500)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("The Matrix"))
				.andExpect(jsonPath("$.genres[0]").value("Action"))
				.andExpect(jsonPath("$.mediaType.name").value("Movie"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		UUID mediaId = UUID.fromString(objectMapper.readTree(created).get("id").asText());

		mockMvc.perform(get("/api/media/{id}", mediaId))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mediaId.toString()))
				.andExpect(jsonPath("$.title").value("The Matrix"))
				.andExpect(jsonPath("$.content").doesNotExist());

		mockMvc.perform(get("/api/media").param("q", "matrix").param("type", "MOVIE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].title").value("The Matrix"))
				.andExpect(jsonPath("$.content[0].inLibrary").value(nullValue()))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(get("/api/media")
						.param("q", "matrix")
						.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].inLibrary").value(false));

		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value("Action"));
	}

	@Test
	void library_upsertListUpdateAndDelete() throws Exception {
		String token = registerAndLogin("carol@example.com", "carol");
		UUID mediaId = createMovie("Heat", 1995, List.of("Crime"), 8.2, 90);

		mockMvc.perform(post("/api/user-media")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "mediaId": "%s",
								  "status": "WATCHING",
								  "rating": 8,
								  "review": "Halfway through"
								}
								""".formatted(mediaId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("WATCHING"))
				.andExpect(jsonPath("$.title").value("Heat"))
				.andExpect(jsonPath("$.genres[0]").value("Crime"))
				.andExpect(jsonPath("$.content").doesNotExist());

		mockMvc.perform(get("/api/user-media")
						.param("status", "WATCHING")
						.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].mediaId").value(mediaId.toString()))
				.andExpect(jsonPath("$.counts.watching").value(1))
				.andExpect(jsonPath("$.counts.completed").value(0));

		mockMvc.perform(post("/api/user-media")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "mediaId": "%s",
								  "status": "COMPLETED",
								  "rating": 9,
								  "review": "Finished it"
								}
								""".formatted(mediaId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.rating").value(9));

		mockMvc.perform(get("/api/user-media").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$.counts.completed").value(1))
				.andExpect(jsonPath("$.counts.watching").value(0));

		mockMvc.perform(get("/api/media")
						.header("Authorization", token)
						.param("q", "Heat"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].inLibrary").value(true));

		mockMvc.perform(delete("/api/user-media/{mediaId}", mediaId)
						.header("Authorization", token))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/user-media").header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isEmpty())
				.andExpect(jsonPath("$.counts.completed").value(0));
	}

	@Test
	void external_searchThenImportIdempotently() throws Exception {
		String token = registerAndLogin("dana@example.com", "dana");
		TmdbMovie details = new TmdbMovie(
				603,
				"Fight Club",
				"An insomniac office worker...",
				"1999-10-15",
				List.of(new TmdbGenre("Drama")),
				8.4,
				26000);
		when(tmdbAdapter.searchMovies("fight club"))
				.thenReturn(new TmdbMovieSearchResponse(List.of(details)));
		when(tmdbAdapter.getMovie("603")).thenReturn(details);

		mockMvc.perform(get("/api/media/search")
						.param("query", "fight club")
						.param("type", "MOVIE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].title").value("Fight Club"))
				.andExpect(jsonPath("$[0].provider").value("TMDB"))
				.andExpect(jsonPath("$[0].externalId").value("movie:603"))
				.andExpect(jsonPath("$.content").doesNotExist());

		String imported = mockMvc.perform(post("/api/media/import")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "tmdb",
								  "externalId": "movie:603"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Fight Club"))
				.andExpect(jsonPath("$.genres[0]").value("Drama"))
				.andExpect(jsonPath("$.rating").value(8.4))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String mediaId = objectMapper.readTree(imported).get("id").asText();

		entityManager.flush();
		entityManager.clear();

		mockMvc.perform(post("/api/media/import")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "provider": "tmdb",
								  "externalId": "movie:603"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(mediaId));

		mockMvc.perform(get("/api/media").param("q", "Fight Club"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].id").value(mediaId));
	}

	@Test
	void recommendations_similarAndPersonalizedFromLibrary() throws Exception {
		String token = registerAndLogin("erin@example.com", "erin");
		UUID liked = createMovie("Liked Action", 2020, List.of("Action"), 9.0, 100);
		UUID close = createMovie("Close Movie", 2020, List.of("Action"), 8.0, 80);
		UUID far = createMovie("Far Movie", 2020, List.of("Drama"), 6.0, 40);
		UUID closeShow = createTv("Close Show", 2020, List.of("Action"), 8.2, 90);

		insertEmbedding(liked, mixed(1f, 0f));
		insertEmbedding(close, mixed(0.95f, 0.05f));
		insertEmbedding(far, mixed(0f, 1f));
		insertEmbedding(closeShow, mixed(0.90f, 0.10f));

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", liked))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies[0].title").value("Close Movie"))
				.andExpect(jsonPath("$.movies[*].title", not(hasItem("Liked Action"))))
				.andExpect(jsonPath("$.tvShows[0].title").value("Close Show"));

		mockMvc.perform(post("/api/user-media")
						.header("Authorization", token)
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "mediaId": "%s",
								  "status": "COMPLETED",
								  "rating": 10
								}
								""".formatted(liked)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "MOVIE")
						.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies[*].title", not(hasItem("Liked Action"))))
				.andExpect(jsonPath("$.movies[0].title").value("Close Movie"))
				.andExpect(jsonPath("$.tvShows[0].title").value("Close Show"));
	}

	private String registerAndLogin(String email, String username) throws Exception {
		mockMvc.perform(post("/api/auth/register")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "username": "%s",
								  "password": "secret123"
								}
								""".formatted(email, username)))
				.andExpect(status().isCreated());
		return login(email, "secret123");
	}

	private String login(String email, String password) throws Exception {
		String body = mockMvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, password)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").isString())
				.andReturn()
				.getResponse()
				.getContentAsString();
		return "Bearer " + objectMapper.readTree(body).get("token").asText();
	}

	private UUID createMovie(
			String title,
			int year,
			List<String> genres,
			double rating,
			int ratingCount) {
		return createMedia(title, year, movieTypeId, genres, rating, ratingCount);
	}

	private UUID createTv(
			String title,
			int year,
			List<String> genres,
			double rating,
			int ratingCount) {
		return createMedia(title, year, tvTypeId, genres, rating, ratingCount);
	}

	private UUID createMedia(
			String title,
			int year,
			UUID typeId,
			List<String> genres,
			double rating,
			int ratingCount) {
		return mediaService.createMedia(new MediaRequestDTO(
				title,
				title + " description",
				(short) year,
				typeId,
				genres,
				rating,
				ratingCount)).id();
	}

	private String createMovieBody(String title, int year, List<String> genres, double rating, int ratingCount) {
		return createMediaBody(title, year, movieTypeId, genres, rating, ratingCount);
	}

	private static String createMediaBody(
			String title,
			int year,
			UUID typeId,
			List<String> genres,
			double rating,
			int ratingCount) {
		String genreJson = genres.stream()
				.map(genre -> "\"" + genre + "\"")
				.reduce((a, b) -> a + ", " + b)
				.map(joined -> "[" + joined + "]")
				.orElse("[]");
		return """
				{
				  "title": "%s",
				  "description": "%s description",
				  "releaseYear": %d,
				  "mediaTypeId": "%s",
				  "genres": %s,
				  "rating": %s,
				  "ratingCount": %d
				}
				""".formatted(title, title, year, typeId, genreJson, rating, ratingCount);
	}

	private void insertEmbedding(UUID mediaId, float[] vector) {
		jdbcTemplate.update(
				"INSERT INTO media_embeddings (media_id, embedding) VALUES (?, CAST(? AS vector))",
				mediaId,
				EmbeddingVectorFormat.toLiteral(vector));
	}

	private static float[] mixed(float first, float second) {
		float[] vector = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		vector[0] = first;
		vector[1] = second;
		return vector;
	}
}
