package com.mediawebapp.service;

import com.mediawebapp.external.adapter.OpenAiEmbeddingAdapter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Calls OpenAI and validates the embedding dimension before anything is stored.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {

	private final OpenAiEmbeddingAdapter openAiEmbeddingAdapter;

	public Optional<float[]> generate(String inputText) {
		try {
			float[] embedding = openAiEmbeddingAdapter.createEmbedding(inputText);
			if (embedding.length != RecommendationScoring.EMBEDDING_DIMENSIONS) {
				log.error(
						"Embedding dimension mismatch: expected {}, got {}",
						RecommendationScoring.EMBEDDING_DIMENSIONS,
						embedding.length);
				return Optional.empty();
			}
			return Optional.of(embedding);
		} catch (RuntimeException exception) {
			log.error("Embedding generation failed: {}", exception.getMessage());
			return Optional.empty();
		}
	}
}
