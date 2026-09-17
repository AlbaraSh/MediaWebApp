package com.mediawebapp.external.dto.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TmdbGenre(String name) {
}
