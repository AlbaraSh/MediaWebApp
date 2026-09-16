package com.mediawebapp.external.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbTvSearchResponse(List<TmdbTv> results) {
}
