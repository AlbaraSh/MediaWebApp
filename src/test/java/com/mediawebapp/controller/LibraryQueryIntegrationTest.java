package com.mediawebapp.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.config.TestContainerConfig;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.UserMediaStatus;
import com.mediawebapp.external.adapter.OpenAiEmbeddingAdapter;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.security.JwtService;
import com.mediawebapp.service.MediaService;
import com.mediawebapp.service.UserMediaService;
import com.mediawebapp.dto.UserMediaRequestDTO;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class LibraryQueryIntegrationTest {

	private static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MediaService mediaService;

	@Autowired
	private UserMediaService userMediaService;

	@Autowired
	private MediaTypeRepository mediaTypeRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@MockitoBean
	private OpenAiEmbeddingAdapter openAiEmbeddingAdapter;

	private UUID movieTypeId;
	private UUID tvTypeId;

	@BeforeEach
	void loadTypes() {
		movieTypeId = mediaTypeRepository.findByName("Movie").orElseThrow().getId();
		tvTypeId = mediaTypeRepository.findByName("TV Show").orElseThrow().getId();
	}

	@Test
	void library_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/user-media"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void library_defaultsToCompletedOrderedByCreatedAtDesc() throws Exception {
		UUID first = createAndList("First Completed", movieTypeId, UserMediaStatus.COMPLETED, 9, "Action");
		UUID second = createAndList("Second Completed", movieTypeId, UserMediaStatus.COMPLETED, 6, "Action");
		UUID third = createAndList("Third Completed", movieTypeId, UserMediaStatus.COMPLETED, null, "Action");
		createAndList("Watching Now", movieTypeId, UserMediaStatus.WATCHING, 10, "Action");
		stampCreatedAt(first, "2026-01-01T00:00:00Z");
		stampCreatedAt(second, "2026-01-02T00:00:00Z");
		stampCreatedAt(third, "2026-01-03T00:00:00Z");

		mockMvc.perform(get("/api/user-media").header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(3)))
				.andExpect(jsonPath("$.content[0].mediaId").value(third.toString()))
				.andExpect(jsonPath("$.content[1].mediaId").value(second.toString()))
				.andExpect(jsonPath("$.content[2].mediaId").value(first.toString()))
				.andExpect(jsonPath("$.content[0].genres[0]").value("Action"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.counts.completed").value(3))
				.andExpect(jsonPath("$.counts.watching").value(1))
				.andExpect(jsonPath("$.counts.planned").value(0))
				.andExpect(jsonPath("$.counts.dropped").value(0));
	}

	@Test
	void getByMediaId_returns200WhenListed() throws Exception {
		UUID listed = createAndList("Listed Film", movieTypeId, UserMediaStatus.WATCHING, 8, "Comedy");

		mockMvc.perform(get("/api/user-media/{mediaId}", listed).header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.mediaId").value(listed.toString()))
				.andExpect(jsonPath("$.status").value("WATCHING"))
				.andExpect(jsonPath("$.title").value("Listed Film"))
				.andExpect(jsonPath("$.genres[0]").value("Comedy"))
				.andExpect(jsonPath("$.content").doesNotExist());
	}

	@Test
	void getByMediaId_returns404WhenNotListed() throws Exception {
		MediaResponseDTO catalogOnly = createMedia("Not Listed", movieTypeId, List.of("Drama"));

		mockMvc.perform(get("/api/user-media/{mediaId}", catalogOnly.id())
						.header("Authorization", bearerToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error").value(
						"User media entry not found for media id: " + catalogOnly.id()))
				.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	void getByMediaId_unauthenticated_returns401() throws Exception {
		mockMvc.perform(get("/api/user-media/{mediaId}", UUID.randomUUID()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void library_statusSwitchAndPaginationKeepCounts() throws Exception {
		for (int i = 0; i < 3; i++) {
			createAndList("Completed " + i, movieTypeId, UserMediaStatus.COMPLETED, 8 - i, "Drama");
		}
		createAndList("Planned One", movieTypeId, UserMediaStatus.PLANNED, null, "Drama");
		createAndList("Dropped One", movieTypeId, UserMediaStatus.DROPPED, 2, "Drama");

		mockMvc.perform(get("/api/user-media")
						.param("status", "COMPLETED")
						.param("page", "0")
						.param("size", "2")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.counts.completed").value(3))
				.andExpect(jsonPath("$.counts.planned").value(1))
				.andExpect(jsonPath("$.counts.dropped").value(1));

		mockMvc.perform(get("/api/user-media")
						.param("status", "COMPLETED")
						.param("page", "1")
						.param("size", "2")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.counts.completed").value(3))
				.andExpect(jsonPath("$.counts.planned").value(1));

		mockMvc.perform(get("/api/user-media")
						.param("status", "PLANNED")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].title").value("Planned One"))
				.andExpect(jsonPath("$.counts.completed").value(3));

		mockMvc.perform(get("/api/user-media")
						.param("page", "99")
						.param("size", "20")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)))
				.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	void library_countsRespectTypeGenreAndQueryButIgnoreStatusAndRatings() throws Exception {
		createAndList("Action Movie", movieTypeId, UserMediaStatus.COMPLETED, 9, "Action");
		createAndList("Drama Movie", movieTypeId, UserMediaStatus.WATCHING, 5, "Drama");
		createAndList("Action Show", tvTypeId, UserMediaStatus.COMPLETED, 8, "Action");

		mockMvc.perform(get("/api/user-media")
						.param("type", "MOVIE")
						.param("genre", "action")
						.param("minRating", "1")
						.param("maxRating", "10")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].title").value("Action Movie"))
				.andExpect(jsonPath("$.counts.completed").value(1))
				.andExpect(jsonPath("$.counts.watching").value(0));

		mockMvc.perform(get("/api/user-media")
						.param("q", "Movie")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].title").value("Action Movie"))
				.andExpect(jsonPath("$.counts.completed").value(1))
				.andExpect(jsonPath("$.counts.watching").value(1));
	}

	@Test
	void library_minGreaterThanMax_returns400() throws Exception {
		mockMvc.perform(get("/api/user-media")
						.param("minRating", "8")
						.param("maxRating", "3")
						.header("Authorization", bearerToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("minRating must be less than or equal to maxRating"));
	}

	@Test
	void library_sizeAboveMax_returns400() throws Exception {
		mockMvc.perform(get("/api/user-media")
						.param("size", "51")
						.header("Authorization", bearerToken()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("size must be between 1 and 50"));
	}

	@Test
	void upsertAndDelete_stillReturnSingleResourceNotPage() throws Exception {
		MediaResponseDTO media = createMedia("Upsert Film", movieTypeId, List.of("Comedy"));

		mockMvc.perform(post("/api/user-media")
						.header("Authorization", bearerToken())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "mediaId": "%s",
								  "status": "WATCHING",
								  "rating": 7
								}
								""".formatted(media.id())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.mediaId").value(media.id().toString()))
				.andExpect(jsonPath("$.status").value("WATCHING"))
				.andExpect(jsonPath("$.genres[0]").value("Comedy"))
				.andExpect(jsonPath("$.content").doesNotExist())
				.andExpect(jsonPath("$.totalElements").doesNotExist());

		mockMvc.perform(delete("/api/user-media/{mediaId}", media.id())
						.header("Authorization", bearerToken()))
				.andExpect(status().isNoContent());
	}

	@Test
	void library_sortByRatingDescPutsHighestFirstAndUnratedLast() throws Exception {
		UUID low = createAndList("Low Score", movieTypeId, UserMediaStatus.COMPLETED, 4, "Action");
		UUID high = createAndList("High Score", movieTypeId, UserMediaStatus.COMPLETED, 9, "Action");
		UUID unrated = createAndList("Unrated", movieTypeId, UserMediaStatus.COMPLETED, null, "Action");

		mockMvc.perform(get("/api/user-media")
						.param("sort", "RATING")
						.param("direction", "DESC")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(3)))
				.andExpect(jsonPath("$.content[0].mediaId").value(high.toString()))
				.andExpect(jsonPath("$.content[1].mediaId").value(low.toString()))
				.andExpect(jsonPath("$.content[2].mediaId").value(unrated.toString()));
	}

	@Test
	void shelfGenres_includeAllStatusesAndIgnoreCatalogOnlyTitles() throws Exception {
		createAndList("Completed Action", movieTypeId, UserMediaStatus.COMPLETED, 8, "Action");
		createAndList("Dropped Horror", movieTypeId, UserMediaStatus.DROPPED, 2, "Horror");
		createMedia("Catalog Only SciFi", movieTypeId, List.of("Sci-Fi"));

		mockMvc.perform(get("/api/user-media/genres").header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0]").value("Action"))
				.andExpect(jsonPath("$[1]").value("Horror"));
	}

	private UUID createAndList(
			String title,
			UUID typeId,
			UserMediaStatus status,
			Integer rating,
			String genre) {
		MediaResponseDTO media = createMedia(title, typeId, List.of(genre));
		userMediaService.upsert(DEV_USER_ID, new UserMediaRequestDTO(
				media.id(), status.name(), rating, null));
		return media.id();
	}

	private void stampCreatedAt(UUID mediaId, String createdAt) {
		jdbcTemplate.update(
				"UPDATE user_media SET created_at = CAST(? AS timestamptz) WHERE user_id = ? AND media_id = ?",
				createdAt,
				DEV_USER_ID,
				mediaId);
	}

	private MediaResponseDTO createMedia(String title, UUID typeId, List<String> genres) {
		return mediaService.createMedia(new MediaRequestDTO(
				title,
				title + " description",
				(short) 2020,
				typeId,
				genres,
				8.0,
				50));
	}

	private String bearerToken() {
		return "Bearer " + jwtService.generateToken(DEV_USER_ID, "dev@mediawebapp.local");
	}
}
