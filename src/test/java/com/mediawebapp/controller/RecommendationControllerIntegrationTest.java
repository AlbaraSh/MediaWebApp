package com.mediawebapp.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.config.TestContainerConfig;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.external.adapter.OpenAiEmbeddingAdapter;
import com.mediawebapp.repository.EmbeddingVectorFormat;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.repository.UserMediaRepository;
import com.mediawebapp.security.JwtService;
import com.mediawebapp.service.MediaService;
import com.mediawebapp.service.RecommendationScoring;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
class RecommendationControllerIntegrationTest {

	private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MediaService mediaService;

	@Autowired
	private MediaTypeRepository mediaTypeRepository;

	@Autowired
	private MediaRepository mediaRepository;

	@Autowired
	private UserMediaRepository userMediaRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private OpenAiEmbeddingAdapter openAiEmbeddingAdapter;

	private UUID movieTypeId;
	private UUID tvTypeId;
	private UUID animeTypeId;
	private UUID gameTypeId;

	@BeforeEach
	void loadTypes() {
		movieTypeId = mediaTypeRepository.findByName("Movie").orElseThrow().getId();
		tvTypeId = mediaTypeRepository.findByName("TV Show").orElseThrow().getId();
		animeTypeId = mediaTypeRepository.findByName("Anime").orElseThrow().getId();
		gameTypeId = mediaTypeRepository.findByName("Game").orElseThrow().getId();
	}

	@Test
	void similar_returnsTopMatchesGroupedAndExcludesSelfAndUnembedded() throws Exception {
		MediaResponseDTO target = createMedia("Target", movieTypeId, List.of("Action"), 8.0, 100);
		MediaResponseDTO close = createMedia("Close Movie", movieTypeId, List.of("Action"), 7.5, 80);
		MediaResponseDTO far = createMedia("Far Movie", movieTypeId, List.of("Drama"), 6.0, 40);
		MediaResponseDTO unembedded = createMedia("No Vector", movieTypeId, List.of("Action"), 9.0, 200);
		MediaResponseDTO closeShow = createMedia("Close Show", tvTypeId, List.of("Action"), 8.2, 90);

		insertEmbedding(target.id(), mixed(1f, 0f));
		insertEmbedding(close.id(), mixed(0.95f, 0.05f));
		insertEmbedding(far.id(), mixed(0f, 1f));
		insertEmbedding(closeShow.id(), mixed(0.90f, 0.10f));

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", target.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies[*].title", contains("Close Movie", "Far Movie")))
				.andExpect(jsonPath("$.movies[*].mediaId", not(hasItem(target.id().toString()))))
				.andExpect(jsonPath("$.movies[*].title", not(hasItem("No Vector"))))
				.andExpect(jsonPath("$.tvShows[*].title", contains("Close Show")))
				.andExpect(jsonPath("$.anime", hasSize(0)))
				.andExpect(jsonPath("$.games", hasSize(0)));

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", unembedded.id()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media is not yet recommendable: " + unembedded.id()))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void similar_returns404WhenMediaMissing() throws Exception {
		UUID missing = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

		mockMvc.perform(get("/api/recommendations/similar/{mediaId}", missing))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value("Media not found with id: " + missing))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void user_requiresTypeAndRejectsInvalid() throws Exception {
		String token = bearerToken();

		mockMvc.perform(get("/api/recommendations/user").header("Authorization", token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Required parameter 'type' is missing"));

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "BOOK")
						.header("Authorization", token))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"));
	}

	@Test
	void user_returnsAvailableCountsWhenCatalogIsSmall() throws Exception {
		MediaResponseDTO liked = createMedia("Liked Movie", movieTypeId, List.of("Action"), 9.0, 500);
		MediaResponseDTO movieA = createMedia("Movie A", movieTypeId, List.of("Action"), 8.0, 100);
		MediaResponseDTO movieB = createMedia("Movie B", movieTypeId, List.of("Drama"), 7.0, 80);
		MediaResponseDTO movieC = createMedia("Movie C", movieTypeId, List.of("Action"), 6.5, 40);
		MediaResponseDTO show = createMedia("Only Show", tvTypeId, List.of("Drama"), 8.1, 70);
		MediaResponseDTO anime = createMedia("Only Anime", animeTypeId, List.of("Action"), 8.4, 90);
		MediaResponseDTO game = createMedia("Only Game", gameTypeId, List.of("Action"), 8.8, 120);
		rate(liked.id(), 10);

		insertEmbedding(liked.id(), mixed(1f, 0f));
		insertEmbedding(movieA.id(), mixed(0.9f, 0.1f));
		insertEmbedding(movieB.id(), mixed(0.8f, 0.2f));
		insertEmbedding(movieC.id(), mixed(0.7f, 0.3f));
		insertEmbedding(show.id(), mixed(0.6f, 0.4f));
		insertEmbedding(anime.id(), mixed(0.5f, 0.5f));
		insertEmbedding(game.id(), mixed(0.4f, 0.6f));

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "MOVIE")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies", hasSize(3)))
				.andExpect(jsonPath("$.movies[*].title", not(hasItem("Liked Movie"))))
				.andExpect(jsonPath("$.tvShows", hasSize(1)))
				.andExpect(jsonPath("$.anime", hasSize(1)))
				.andExpect(jsonPath("$.games", hasSize(1)));
	}

	@Test
	void user_appliesTwentyFiveFiveFiveDistribution() throws Exception {
		MediaResponseDTO liked = createMedia("Seed Movie", movieTypeId, List.of("Action"), 9.0, 500);
		rate(liked.id(), 9);
		insertEmbedding(liked.id(), mixed(1f, 0f));

		for (int i = 0; i < 6; i++) {
			insertEmbedding(
					createMedia("Dist Movie " + i, movieTypeId, List.of("Action"), 8.0, 50).id(),
					mixed(0.85f, 0.15f));
		}
		for (int i = 0; i < 20; i++) {
			float tilt = i * 0.01f;
			insertEmbedding(
					createMedia("Dist Show " + i, tvTypeId, List.of("Action"), 8.0, 50).id(),
					mixed(0.9f - tilt, 0.1f + tilt));
		}
		for (int i = 0; i < 6; i++) {
			insertEmbedding(
					createMedia("Dist Anime " + i, animeTypeId, List.of("Action"), 8.0, 50).id(),
					mixed(0.6f, 0.4f));
			insertEmbedding(
					createMedia("Dist Game " + i, gameTypeId, List.of("Action"), 8.0, 50).id(),
					mixed(0.5f, 0.5f));
		}

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "TV")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tvShows", hasSize(20)))
				.andExpect(jsonPath("$.movies", hasSize(5)))
				.andExpect(jsonPath("$.anime", hasSize(5)))
				.andExpect(jsonPath("$.games", hasSize(5)));
	}

	@Test
	void user_fallsBackToTopRatedWhenNoPositiveItems() throws Exception {
		createMedia("High Movie", movieTypeId, List.of("Action"), 9.5, 2000);
		createMedia("Low Movie", movieTypeId, List.of("Drama"), 5.0, 10);
		MediaResponseDTO alreadyListed = createMedia("Listed Movie", movieTypeId, List.of("Action"), 9.9, 5000);
		rate(alreadyListed.id(), 4);
		createMedia("Top Show", tvTypeId, List.of("Drama"), 9.0, 800);

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "MOVIE")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.movies[0].title").value("High Movie"))
				.andExpect(jsonPath("$.movies[*].title", not(hasItem("Listed Movie"))))
				.andExpect(jsonPath("$.tvShows[0].title").value("Top Show"));
	}

	@Test
	void user_ignoresGenreOverlapWhenTypeHasNoHistory() throws Exception {
		MediaResponseDTO likedMovie = createMedia("Liked Action", movieTypeId, List.of("Action"), 9.0, 100);
		MediaResponseDTO actionTv = createMedia("Action TV", tvTypeId, List.of("Action"), 8.0, 100);
		MediaResponseDTO similarTv = createMedia("Similar TV", tvTypeId, List.of("Comedy"), 8.0, 100);
		rate(likedMovie.id(), 10);
		insertEmbedding(likedMovie.id(), mixed(1f, 0f));
		insertEmbedding(actionTv.id(), mixed(0.80f, 0.20f));
		insertEmbedding(similarTv.id(), mixed(0.90f, 0.10f));

		mockMvc.perform(get("/api/recommendations/user")
						.param("type", "TV")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.tvShows[*].title", contains("Similar TV", "Action TV")));
	}

	private MediaResponseDTO createMedia(
			String title,
			UUID typeId,
			List<String> genres,
			Double rating,
			Integer ratingCount) {
		return mediaService.createMedia(new MediaRequestDTO(
				title,
				title + " description",
				(short) 2020,
				typeId,
				genres,
				rating,
				ratingCount));
	}

	private void insertEmbedding(UUID mediaId, float[] vector) {
		jdbcTemplate.update(
				"INSERT INTO media_embeddings (media_id, embedding) VALUES (?, CAST(? AS vector))",
				mediaId,
				EmbeddingVectorFormat.toLiteral(vector));
	}

	private void rate(UUID mediaId, int rating) {
		Media media = mediaRepository.findById(mediaId).orElseThrow();
		UserMedia entry = new UserMedia();
		entry.setUserId(DEV_USER_ID);
		entry.setMedia(media);
		entry.setStatus(UserMediaStatus.COMPLETED);
		entry.setRating(rating);
		userMediaRepository.saveAndFlush(entry);
	}

	private String bearerToken() {
		return "Bearer " + jwtService.generateToken(DEV_USER_ID, "dev@mediawebapp.local");
	}

	private static float[] mixed(float first, float second) {
		float[] vector = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		vector[0] = first;
		vector[1] = second;
		return vector;
	}
}
