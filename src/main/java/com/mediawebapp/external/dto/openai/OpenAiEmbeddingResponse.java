package com.mediawebapp.external.dto.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenAiEmbeddingResponse(List<OpenAiEmbeddingData> data) {
}
