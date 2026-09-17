package com.mediawebapp.external.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanNamedEntry(String name) {
}
