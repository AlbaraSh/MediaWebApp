package com.mediawebapp.external.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * TMDB movie payload (search hit or {@code /movie/{id}} details).
 * Search typically omits {@code genres}; details include {@code genres[].name}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovie(
		Integer id,
		String title,
		String overview,
		@JsonProperty("release_date") String releaseDate,
		List<TmdbGenre> genres,
		@JsonProperty("vote_average") Double voteAverage,
		@JsonProperty("vote_count") Integer voteCount,
		@JsonProperty("poster_path") String posterPath
) {
	public TmdbMovie(Integer id, String title, String overview, String releaseDate) {
		this(id, title, overview, releaseDate, null, null, null, null);
	}
}
