package com.mediawebapp.external.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanAnime(
		@JsonProperty("mal_id") Integer malId,
		String title,
		String synopsis,
		Integer year,
		JikanAired aired,
		List<JikanNamedEntry> genres,
		List<JikanNamedEntry> themes,
		List<JikanNamedEntry> demographics,
		Double score,
		@JsonProperty("scored_by") Integer scoredBy
) {
	public JikanAnime(Integer malId, String title, String synopsis, Integer year, JikanAired aired) {
		this(malId, title, synopsis, year, aired, null, null, null, null, null);
	}
}
