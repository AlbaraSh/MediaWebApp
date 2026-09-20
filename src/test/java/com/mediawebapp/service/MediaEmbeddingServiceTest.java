package com.mediawebapp.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mediawebapp.repository.MediaEmbeddingRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MediaEmbeddingServiceTest {

	@Mock
	private EmbeddingService embeddingService;

	@Mock
	private MediaEmbeddingRepository mediaEmbeddingRepository;

	private MediaEmbeddingService mediaEmbeddingService;
	private UUID mediaId;

	@BeforeEach
	void setUp() {
		mediaEmbeddingService = new MediaEmbeddingService(embeddingService, mediaEmbeddingRepository);
		mediaId = UUID.randomUUID();
	}

	@Test
	void generateAndStore_persistsWhenEmbeddingSucceeds() {
		float[] vector = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		Arrays.fill(vector, 0.02f);
		when(embeddingService.generate("Dune. Desert planet. Sci-Fi")).thenReturn(Optional.of(vector));

		mediaEmbeddingService.generateAndStore(mediaId, "Dune", "Desert planet", List.of("Sci-Fi"));

		verify(mediaEmbeddingRepository).upsert(mediaId, vector);
	}

	@Test
	void generateAndStore_skipsPersistWhenGenerationFails() {
		when(embeddingService.generate(anyString())).thenReturn(Optional.empty());

		mediaEmbeddingService.generateAndStore(mediaId, "Dune", "Desert planet", List.of("Sci-Fi"));

		verify(mediaEmbeddingRepository, never()).upsert(any(), any());
	}

	@Test
	void generateAndStore_doesNotThrowWhenRepositoryFails() {
		float[] vector = new float[RecommendationScoring.EMBEDDING_DIMENSIONS];
		when(embeddingService.generate(anyString())).thenReturn(Optional.of(vector));
		org.mockito.Mockito.doThrow(new RuntimeException("db down"))
				.when(mediaEmbeddingRepository)
				.upsert(eq(mediaId), any());

		mediaEmbeddingService.generateAndStore(mediaId, "Dune", null, List.of());
	}
}
