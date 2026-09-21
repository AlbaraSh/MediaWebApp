package com.mediawebapp.external.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanImageSet(
		@JsonProperty("image_url") String imageUrl,
		@JsonProperty("large_image_url") String largeImageUrl
) {
}
