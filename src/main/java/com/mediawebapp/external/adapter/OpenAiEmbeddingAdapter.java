package com.mediawebapp.external.adapter;

import com.mediawebapp.config.OpenAiProperties;
import com.mediawebapp.external.dto.openai.OpenAiEmbeddingData;
import com.mediawebapp.external.dto.openai.OpenAiEmbeddingRequest;
import com.mediawebapp.external.dto.openai.OpenAiEmbeddingResponse;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for the OpenAI embeddings API. Returns the raw vector only.
 */
@Component
public class OpenAiEmbeddingAdapter {

	static final String MODEL = "text-embedding-3-small";

	private final RestClient restClient;
	private final String apiKey;

	public OpenAiEmbeddingAdapter(RestClient.Builder restClientBuilder, OpenAiProperties properties) {
		this.apiKey = properties.key();
		this.restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.build();
	}

	public float[] createEmbedding(String input) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException("OpenAI API key is not configured");
		}
		OpenAiEmbeddingResponse response;
		try {
			response = restClient.post()
					.uri("/embeddings")
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
					.contentType(MediaType.APPLICATION_JSON)
					.body(new OpenAiEmbeddingRequest(MODEL, input))
					.retrieve()
					.onStatus(status -> status.isError(), (request, httpResponse) -> {
						throw new IllegalStateException(
								"OpenAI embeddings request failed with status "
										+ httpResponse.getStatusCode().value());
					})
					.body(OpenAiEmbeddingResponse.class);
		} catch (RestClientException exception) {
			throw new IllegalStateException("OpenAI embeddings request failed", exception);
		}
		List<Double> values = extractEmbedding(response);
		float[] embedding = new float[values.size()];
		for (int i = 0; i < values.size(); i++) {
			embedding[i] = values.get(i).floatValue();
		}
		return embedding;
	}

	private static List<Double> extractEmbedding(OpenAiEmbeddingResponse response) {
		if (response == null || response.data() == null || response.data().isEmpty()) {
			throw new IllegalStateException("OpenAI embeddings response contained no data");
		}
		OpenAiEmbeddingData first = response.data().get(0);
		if (first == null || first.embedding() == null || first.embedding().isEmpty()) {
			throw new IllegalStateException("OpenAI embeddings response contained no vector");
		}
		return first.embedding();
	}
}
