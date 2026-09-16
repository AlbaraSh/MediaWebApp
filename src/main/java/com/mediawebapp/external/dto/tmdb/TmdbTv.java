package com.mediawebapp.external.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTv(
		Integer id,
		String name,
		String overview,
		@JsonProperty("first_air_date") String firstAirDate,
		List<TmdbGenre> genres,
		@JsonProperty("vote_average") Double voteAverage,
		@JsonProperty("vote_count") Integer voteCount
) {
	public TmdbTv(Integer id, String name, String overview, String firstAirDate) {
		this(id, name, overview, firstAirDate, null, null, null);
	}
}
