package com.mediawebapp.external.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record JikanSearchResponse(List<JikanAnime> data) {
}
