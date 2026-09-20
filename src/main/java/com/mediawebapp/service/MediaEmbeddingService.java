package com.mediawebapp.service;

import com.mediawebapp.repository.MediaEmbeddingRepository;
import java.util.Collection;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Generates and persists a media embedding. Failures are logged and skipped
 * so catalog creates/imports still succeed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaEmbeddingService {

	private final EmbeddingService embeddingService;
	private final MediaEmbeddingRepository mediaEmbeddingRepository;

	public void generateAndStore(UUID mediaId, String title, String description, Collection<String> genreNames) {
		try {
			String input = EmbeddingTextBuilder.build(title, description, genreNames);
			if (input.isBlank()) {
				log.error("Skipping embedding for media {}: input text was empty", mediaId);
				return;
			}
			embeddingService.generate(input).ifPresentOrElse(
					embedding -> mediaEmbeddingRepository.upsert(mediaId, embedding),
					() -> log.error("Skipping embedding persist for media {}", mediaId));
		} catch (RuntimeException exception) {
			log.error("Failed to generate embedding for media {}: {}", mediaId, exception.getMessage());
		}
	}
}
