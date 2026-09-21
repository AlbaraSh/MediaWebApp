package com.mediawebapp.external.dto.rawg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgGame(
		Integer id,
		String name,
		String released,
		@JsonProperty("description_raw") String descriptionRaw,
		List<RawgNamedEntry> genres,
		Double rating,
		@JsonProperty("ratings_count") Integer ratingsCount,
		@JsonProperty("background_image") String backgroundImage
) {
	public RawgGame(Integer id, String name, String released, String descriptionRaw) {
		this(id, name, released, descriptionRaw, null, null, null, null);
	}
}
