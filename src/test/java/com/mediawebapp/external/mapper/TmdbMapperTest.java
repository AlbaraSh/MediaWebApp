package com.mediawebapp.external.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.tmdb.TmdbGenre;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import java.util.List;
import org.junit.jupiter.api.Test;

class TmdbMapperTest {

	private final TmdbMapper mapper = new TmdbMapper();

	@Test
	void toMovieDto_normalizesFieldsAndPrefixesExternalId() {
		TmdbMovie movie = new TmdbMovie(550, "Fight Club", "An insomniac office worker.", "1999-10-15");

		ExternalMediaDTO dto = mapper.toMovieDto(movie);

		assertThat(dto.title()).isEqualTo("Fight Club");
		assertThat(dto.description()).isEqualTo("An insomniac office worker.");
		assertThat(dto.releaseYear()).isEqualTo((short) 1999);
		assertThat(dto.mediaType()).isEqualTo("Movie");
		assertThat(dto.provider()).isEqualTo("TMDB");
		assertThat(dto.externalId()).isEqualTo("movie:550");
		assertThat(dto.genres()).isEmpty();
		assertThat(dto.externalRating()).isNull();
		assertThat(dto.externalRatingCount()).isNull();
	}

	@Test
	void toMovieDto_extractsGenreNamesAndVotesFromDetails() {
		TmdbMovie movie = new TmdbMovie(
				550,
				"Fight Club",
				"An insomniac office worker.",
				"1999-10-15",
				List.of(new TmdbGenre("Drama"), new TmdbGenre(" Thriller "), new TmdbGenre("  ")),
				8.433,
				27238);

		ExternalMediaDTO dto = mapper.toMovieDto(movie);

		assertThat(dto.genres()).containsExactly("Drama", "Thriller");
		assertThat(dto.externalRating()).isEqualTo(8.433);
		assertThat(dto.externalRatingCount()).isEqualTo(27238);
	}

	@Test
	void toTvDto_usesNameAndFirstAirDate() {
		TmdbTv show = new TmdbTv(1396, "Breaking Bad", "A chemistry teacher.", "2008-01-20");

		ExternalMediaDTO dto = mapper.toTvDto(show);

		assertThat(dto.title()).isEqualTo("Breaking Bad");
		assertThat(dto.releaseYear()).isEqualTo((short) 2008);
		assertThat(dto.mediaType()).isEqualTo("TV Show");
		assertThat(dto.provider()).isEqualTo("TMDB");
		assertThat(dto.externalId()).isEqualTo("tv:1396");
	}

	@Test
	void toMovieDto_passesNullReleaseYearWhenDateMissing() {
		TmdbMovie movie = new TmdbMovie(1, "Untitled", "Overview", null);

		ExternalMediaDTO dto = mapper.toMovieDto(movie);

		assertThat(dto.releaseYear()).isNull();
		assertThat(dto.title()).isEqualTo("Untitled");
	}

	@Test
	void toMovieDto_passesNullReleaseYearWhenDateBlank() {
		TmdbMovie movie = new TmdbMovie(1, "Untitled", "", "   ");

		ExternalMediaDTO dto = mapper.toMovieDto(movie);

		assertThat(dto.releaseYear()).isNull();
		assertThat(dto.description()).isNull();
	}

	@Test
	void toMovieDtos_skipsNullIdsAndMapsRemaining() {
		TmdbMovieSearchResponse response = new TmdbMovieSearchResponse(List.of(
				new TmdbMovie(null, "Skip", null, null),
				new TmdbMovie(550, "Fight Club", "Overview", "1999-10-15")
		));

		List<ExternalMediaDTO> dtos = mapper.toMovieDtos(response);

		assertThat(dtos).hasSize(1);
		assertThat(dtos.get(0).externalId()).isEqualTo("movie:550");
	}

	@Test
	void toTvDtos_handlesNullResponse() {
		assertThat(mapper.toTvDtos(null)).isEmpty();
		assertThat(mapper.toTvDtos(new TmdbTvSearchResponse(null))).isEmpty();
	}
}
