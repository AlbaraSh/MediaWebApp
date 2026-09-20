package com.mediawebapp.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
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
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import com.mediawebapp.repository.UserMediaRepository;
import com.mediawebapp.security.JwtService;
import com.mediawebapp.service.MediaService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestContainerConfig.class)
@ActiveProfiles("test")
@Transactional
class DiscoverQueryIntegrationTest {

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
	private JwtService jwtService;

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
	void discover_emptyCatalog_returnsEmptyPage() throws Exception {
		mockMvc.perform(get("/api/media"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(0))
				.andExpect(jsonPath("$.totalPages").value(0));
	}

	@Test
	void discover_paginatesAndAppliesFiltersToTotals() throws Exception {
		createMedia("Alpha", movieTypeId, List.of("Action"), 8.0, 100, (short) 1999);
		createMedia("Beta", movieTypeId, List.of("Drama"), 7.0, 80, (short) 1999);
		createMedia("Gamma", movieTypeId, List.of("Action"), 6.0, 40, (short) 2001);
		createMedia("Delta Show", tvTypeId, List.of("Action"), 9.0, 50, (short) 1999);

		mockMvc.perform(get("/api/media")
						.param("type", "MOVIE")
						.param("page", "0")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(2)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/media")
						.param("type", "MOVIE")
						.param("page", "1")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		mockMvc.perform(get("/api/media")
						.param("type", "MOVIE")
						.param("page", "99")
						.param("size", "2"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(0)))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));
	}

	@Test
	void discover_qualityOrdersUnratedLastForBothDirections() throws Exception {
		createMedia("High", movieTypeId, List.of("Action"), 9.0, 100, (short) 2000);
		createMedia("Low", movieTypeId, List.of("Action"), 5.0, 100, (short) 2000);
		createMedia("Unrated", movieTypeId, List.of("Action"), null, null, (short) 2000);

		mockMvc.perform(get("/api/media").param("sort", "QUALITY").param("direction", "DESC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].title", contains("High", "Low", "Unrated")));

		mockMvc.perform(get("/api/media").param("sort", "QUALITY").param("direction", "ASC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].title", contains("Low", "High", "Unrated")));
	}

	@Test
	void discover_qualityRanksHighVotesAboveSparsePerfectScore() throws Exception {
		createMedia("Hyped", movieTypeId, List.of("Action"), 9.8, 5, (short) 2020);
		createMedia("Solid", movieTypeId, List.of("Action"), 8.5, 2000, (short) 2020);
		createMedia("Filler", movieTypeId, List.of("Drama"), 6.0, 100, (short) 2020);

		mockMvc.perform(get("/api/media").param("sort", "QUALITY").param("direction", "DESC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Solid"))
				.andExpect(jsonPath("$.content[*].title", contains("Solid", "Hyped", "Filler")));
	}

	@Test
	void discover_filtersByGenreYearAndTitle() throws Exception {
		createMedia("The Matrix", movieTypeId, List.of("Action"), 8.7, 100, (short) 1999);
		createMedia("Matrix Reloaded", movieTypeId, List.of("Sci-Fi"), 7.0, 80, (short) 2003);
		createMedia("Heat", movieTypeId, List.of("Action"), 8.2, 90, (short) 1995);

		mockMvc.perform(get("/api/media")
						.param("genre", "action")
						.param("year", "1999")
						.param("q", "matrix"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.content[0].title").value("The Matrix"))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void discover_invalidTypeAndSort_return400() throws Exception {
		mockMvc.perform(get("/api/media").param("type", "BOOK"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"))
				.andExpect(jsonPath("$.status").value(400));

		mockMvc.perform(get("/api/media").param("sort", "POPULARITY"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("Invalid sort. Must be one of: QUALITY, YEAR, TITLE"))
				.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	void discover_sizeAboveMax_returns400() throws Exception {
		mockMvc.perform(get("/api/media").param("size", "51"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error").value("size must be between 1 and 50"));
	}

	@Test
	void discover_unauthenticatedInLibraryIsNull_authenticatedSetsFlagsForPageOnly() throws Exception {
		MediaResponseDTO listed = createMedia("Listed", movieTypeId, List.of("Action"), 8.0, 50, (short) 2000);
		createMedia("Other", movieTypeId, List.of("Action"), 7.0, 50, (short) 2000);
		addToLibrary(listed.id());

		mockMvc.perform(get("/api/media").param("sort", "TITLE").param("direction", "ASC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].inLibrary").value(nullValue()))
				.andExpect(jsonPath("$.content[1].inLibrary").value(nullValue()));

		mockMvc.perform(get("/api/media")
						.param("sort", "TITLE")
						.param("direction", "ASC")
						.header("Authorization", bearerToken()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].title").value("Listed"))
				.andExpect(jsonPath("$.content[0].inLibrary").value(true))
				.andExpect(jsonPath("$.content[1].title").value("Other"))
				.andExpect(jsonPath("$.content[1].inLibrary").value(false));
	}

	@Test
	void getById_stillReturnsSingleDtoNotPage() throws Exception {
		MediaResponseDTO created = createMedia("Solo", movieTypeId, List.of("Drama"), 8.0, 10, (short) 2011);

		mockMvc.perform(get("/api/media/{id}", created.id()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Solo"))
				.andExpect(jsonPath("$.content").doesNotExist())
				.andExpect(jsonPath("$.totalElements").doesNotExist());
	}

	@Test
	void genres_returnsDistinctUsedNamesAlphabetically() throws Exception {
		createMedia("A", movieTypeId, List.of("drama", "Action"), 8.0, 10, (short) 2000);
		createMedia("B", movieTypeId, List.of("Action"), 7.0, 10, (short) 2000);

		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", contains("Action", "Drama")));
	}

	@Test
	void genres_emptyCatalog_returnsEmptyArray() throws Exception {
		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	private MediaResponseDTO createMedia(
			String title,
			UUID typeId,
			List<String> genres,
			Double rating,
			Integer ratingCount,
			Short year) {
		return mediaService.createMedia(new MediaRequestDTO(
				title,
				title + " description",
				year,
				typeId,
				genres,
				rating,
				ratingCount));
	}

	private void addToLibrary(UUID mediaId) {
		Media media = mediaRepository.findById(mediaId).orElseThrow();
		UserMedia entry = new UserMedia();
		entry.setUserId(DEV_USER_ID);
		entry.setMedia(media);
		entry.setStatus(UserMediaStatus.COMPLETED);
		entry.setRating(8);
		userMediaRepository.saveAndFlush(entry);
	}

	private String bearerToken() {
		return "Bearer " + jwtService.generateToken(DEV_USER_ID, "dev@mediawebapp.local");
	}
}
