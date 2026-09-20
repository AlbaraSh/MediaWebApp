package com.mediawebapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.external.adapter.OpenAiEmbeddingAdapter;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

	@Mock
	private OpenAiEmbeddingAdapter adapter;

	private EmbeddingService embeddingService;

	@BeforeEach
	void setUp() {
		embeddingService = new EmbeddingService(adapter);
	}

	@Test
	void generate_returnsVectorWhenDimensionMatches() {
		float[] vector = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		Arrays.fill(vector, 0.01f);
		when(adapter.createEmbedding("Dune. Desert planet")).thenReturn(vector);

		Optional<float[]> result = embeddingService.generate("Dune. Desert planet");

		assertThat(result).isPresent();
		assertThat(result.get()).hasSize(1536);
		assertThat(result.get()[0]).isEqualTo(0.01f);
	}

	@Test
	void generate_skipsWhenDimensionDoesNotMatch() {
		when(adapter.createEmbedding(anyString())).thenReturn(new float[] {0.1f, 0.2f, 0.3f});

		assertThat(embeddingService.generate("Dune")).isEmpty();
	}

	@Test
	void generate_skipsWhenAdapterFails() {
		when(adapter.createEmbedding(anyString())).thenThrow(new IllegalStateException("OpenAI down"));

		assertThat(embeddingService.generate("Dune")).isEmpty();
		verify(adapter).createEmbedding("Dune");
	}
}
