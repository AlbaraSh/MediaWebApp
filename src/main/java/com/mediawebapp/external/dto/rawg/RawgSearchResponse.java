package com.mediawebapp.external.dto.rawg;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawgSearchResponse(List<RawgGame> results) {
}
