package com.mediawebapp.external.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.jikan.JikanAired;
import com.mediawebapp.external.dto.jikan.JikanAnime;
import com.mediawebapp.external.dto.jikan.JikanImageSet;
import com.mediawebapp.external.dto.jikan.JikanImages;
import com.mediawebapp.external.dto.jikan.JikanNamedEntry;
import com.mediawebapp.external.dto.jikan.JikanSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class JikanMapperTest {

	private final JikanMapper mapper = new JikanMapper();

	@Test
	void toDto_usesYearField() {
		JikanAnime anime = new JikanAnime(
				1, "Cowboy Bebop", "The futuristic misadventures.", 1998, null);

		ExternalMediaDTO dto = mapper.toDto(anime);

		assertThat(dto.title()).isEqualTo("Cowboy Bebop");
		assertThat(dto.description()).isEqualTo("The futuristic misadventures.");
		assertThat(dto.releaseYear()).isEqualTo((short) 1998);
		assertThat(dto.mediaType()).isEqualTo("Anime");
		assertThat(dto.provider()).isEqualTo("JIKAN");
		assertThat(dto.externalId()).isEqualTo("1");
		assertThat(dto.genres()).isEmpty();
		assertThat(dto.externalRating()).isNull();
		assertThat(dto.posterUrl()).isNull();
	}

	@Test
	void toDto_mergesGenresThemesAndDemographicsAndScore() {
		JikanAnime anime = new JikanAnime(
				1,
				"Cowboy Bebop",
				"The futuristic misadventures.",
				1998,
				null,
				List.of(new JikanNamedEntry("Action"), new JikanNamedEntry("Sci-Fi")),
				List.of(new JikanNamedEntry("Adult Cast"), new JikanNamedEntry("Action")),
				List.of(new JikanNamedEntry("Seinen"), new JikanNamedEntry("  ")),
				8.75,
				900000,
				null);

		ExternalMediaDTO dto = mapper.toDto(anime);

		assertThat(dto.genres()).containsExactly("Action", "Sci-Fi", "Adult Cast", "Seinen");
		assertThat(dto.externalRating()).isEqualTo(8.75);
		assertThat(dto.externalRatingCount()).isEqualTo(900000);
	}

	@Test
	void toDto_fallsBackToAiredFromWhenYearMissing() {
		JikanAnime anime = new JikanAnime(
				1, "Cowboy Bebop", "Synopsis", null, new JikanAired("1998-04-03T00:00:00+00:00"));

		ExternalMediaDTO dto = mapper.toDto(anime);

		assertThat(dto.releaseYear()).isEqualTo((short) 1998);
	}

	@Test
	void toDto_passesNullReleaseYearWhenNoDateAvailable() {
		JikanAnime anime = new JikanAnime(5, "Unknown", null, null, null);

		ExternalMediaDTO dto = mapper.toDto(anime);

		assertThat(dto.releaseYear()).isNull();
		assertThat(dto.description()).isNull();
	}

	@Test
	void toDtos_handlesNullResponse() {
		assertThat(mapper.toDtos(null)).isEmpty();
		assertThat(mapper.toDtos(new JikanSearchResponse(null))).isEmpty();
		assertThat(mapper.toDtos(new JikanSearchResponse(List.of()))).isEmpty();
	}

	@Test
	void toDto_prefersLargeJpgImageUrl() {
		JikanAnime anime = new JikanAnime(
				1,
				"Cowboy Bebop",
				"Synopsis",
				1998,
				null,
				null,
				null,
				null,
				null,
				null,
				new JikanImages(new JikanImageSet(
						"https://cdn.myanimelist.net/images/anime/small.jpg",
						"https://cdn.myanimelist.net/images/anime/large.jpg")));

		assertThat(mapper.toDto(anime).posterUrl())
				.isEqualTo("https://cdn.myanimelist.net/images/anime/large.jpg");
	}

	@Test
	void toDto_fallsBackToJpgImageUrlWhenLargeMissing() {
		JikanAnime anime = new JikanAnime(
				1,
				"Cowboy Bebop",
				"Synopsis",
				1998,
				null,
				null,
				null,
				null,
				null,
				null,
				new JikanImages(new JikanImageSet(
						"https://cdn.myanimelist.net/images/anime/small.jpg",
						null)));

		assertThat(mapper.toDto(anime).posterUrl())
				.isEqualTo("https://cdn.myanimelist.net/images/anime/small.jpg");
	}

	@Test
	void toDto_mapsMissingImagesToNull() {
		JikanAnime anime = new JikanAnime(5, "Unknown", null, null, null);

		assertThat(mapper.toDto(anime).posterUrl()).isNull();
	}
}
