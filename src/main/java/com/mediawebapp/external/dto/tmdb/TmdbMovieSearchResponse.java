package com.mediawebapp.external.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbMovieSearchResponse(List<TmdbMovie> results) {
}
