package com.mediawebapp.external.mapper;

import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.external.dto.tmdb.TmdbGenre;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TmdbMapper {

	public List<ExternalMediaDTO> toMovieDtos(TmdbMovieSearchResponse response) {
		if (response == null || response.results() == null) {
			return List.of();
		}
		return response.results().stream()
				.filter(movie -> movie != null && movie.id() != null)
				.map(this::toMovieDto)
				.toList();
	}

	public List<ExternalMediaDTO> toTvDtos(TmdbTvSearchResponse response) {
		if (response == null || response.results() == null) {
			return List.of();
		}
		return response.results().stream()
				.filter(show -> show != null && show.id() != null)
				.map(this::toTvDto)
				.toList();
	}

	public ExternalMediaDTO toMovieDto(TmdbMovie movie) {
		return new ExternalMediaDTO(
				blankToNull(movie.title()),
				blankToNull(movie.overview()),
				parseYear(movie.releaseDate()),
				"Movie",
				"TMDB",
				"movie:" + movie.id(),
				genreNames(movie.genres()),
				movie.voteAverage(),
				movie.voteCount()
		);
	}

	public ExternalMediaDTO toTvDto(TmdbTv show) {
		return new ExternalMediaDTO(
				blankToNull(show.name()),
				blankToNull(show.overview()),
				parseYear(show.firstAirDate()),
				"TV Show",
				"TMDB",
				"tv:" + show.id(),
				genreNames(show.genres()),
				show.voteAverage(),
				show.voteCount()
		);
	}

	private static List<String> genreNames(List<TmdbGenre> genres) {
		if (genres == null || genres.isEmpty()) {
			return List.of();
		}
		List<String> names = new ArrayList<>();
		for (TmdbGenre genre : genres) {
			if (genre != null && genre.name() != null && !genre.name().isBlank()) {
				names.add(genre.name().trim());
			}
		}
		return List.copyOf(names);
	}

	private static String blankToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}

	private static Short parseYear(String date) {
		if (date == null || date.isBlank() || date.length() < 4) {
			return null;
		}
		try {
			return Short.parseShort(date.substring(0, 4));
		} catch (NumberFormatException exception) {
			return null;
		}
	}
}
