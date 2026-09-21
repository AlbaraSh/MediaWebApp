package com.mediawebapp.external.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.rawg.RawgNamedEntry;
import com.mediawebapp.external.dto.rawg.RawgSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class RawgMapperTest {

	private final RawgMapper mapper = new RawgMapper();

	@Test
	void toDto_usesDescriptionRawAndReleasedYear() {
		RawgGame game = new RawgGame(3498, "Grand Theft Auto V", "2013-09-17", "Plain text synopsis");

		ExternalMediaDTO dto = mapper.toDto(game);

		assertThat(dto.title()).isEqualTo("Grand Theft Auto V");
		assertThat(dto.description()).isEqualTo("Plain text synopsis");
		assertThat(dto.releaseYear()).isEqualTo((short) 2013);
		assertThat(dto.mediaType()).isEqualTo("Game");
		assertThat(dto.provider()).isEqualTo("RAWG");
		assertThat(dto.externalId()).isEqualTo("3498");
		assertThat(dto.genres()).isEmpty();
		assertThat(dto.externalRating()).isNull();
		assertThat(dto.posterUrl()).isNull();
	}

	@Test
	void toDto_mapsGenresOnlyAndConvertsRatingToTenPointScale() {
		RawgGame game = new RawgGame(
				3498,
				"Grand Theft Auto V",
				"2013-09-17",
				"Plain text synopsis",
				List.of(new RawgNamedEntry("Action"), new RawgNamedEntry("Adventure")),
				4.47,
				6600,
				null);

		ExternalMediaDTO dto = mapper.toDto(game);

		assertThat(dto.genres()).containsExactly("Action", "Adventure");
		assertThat(dto.externalRating()).isEqualTo(8.9);
		assertThat(dto.externalRatingCount()).isEqualTo(6600);
	}

	@Test
	void toDto_passesNullReleaseYearWhenReleasedMissing() {
		RawgGame game = new RawgGame(1, "Unreleased", null, "Soon");

		ExternalMediaDTO dto = mapper.toDto(game);

		assertThat(dto.releaseYear()).isNull();
		assertThat(dto.description()).isEqualTo("Soon");
	}

	@Test
	void toDto_doesNotMapHtmlDescriptionField() {
		RawgGame game = new RawgGame(1, "Game", "2020-01-01", null);

		ExternalMediaDTO dto = mapper.toDto(game);

		assertThat(dto.description()).isNull();
	}

	@Test
	void toDtos_handlesNullResponse() {
		assertThat(mapper.toDtos(null)).isEmpty();
		assertThat(mapper.toDtos(new RawgSearchResponse(null))).isEmpty();
		assertThat(mapper.toDtos(new RawgSearchResponse(List.of()))).isEmpty();
	}

	@Test
	void toDto_mapsBackgroundImage() {
		RawgGame game = new RawgGame(
				3498,
				"Grand Theft Auto V",
				"2013-09-17",
				"Plain text synopsis",
				null,
				null,
				null,
				"https://media.rawg.io/media/games/456/456.jpg");

		assertThat(mapper.toDto(game).posterUrl())
				.isEqualTo("https://media.rawg.io/media/games/456/456.jpg");
	}

	@Test
	void toDto_mapsBlankBackgroundImageToNull() {
		RawgGame game = new RawgGame(
				1, "Game", "2020-01-01", null, null, null, null, "  ");

		assertThat(mapper.toDto(game).posterUrl()).isNull();
	}
}
