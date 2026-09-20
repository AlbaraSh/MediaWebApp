package com.mediawebapp.service;

import com.mediawebapp.dto.CatalogType;
import com.mediawebapp.dto.RecommendationItemDTO;
import com.mediawebapp.dto.RecommendationResponseDTO;
import com.mediawebapp.entity.Genre;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.UserMedia;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.repository.EmbeddingVectorFormat;
import com.mediawebapp.repository.MediaEmbeddingRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.repository.MediaSimilarityRow;
import com.mediawebapp.repository.UserMediaRepository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecommendationService {

	static final int SIMILAR_LIMIT = 20;
	static final int CANDIDATES_PER_TYPE = 50;
	static final int REQUESTED_TYPE_LIMIT = 20;
	static final int OTHER_TYPE_LIMIT = 5;
	static final int POSITIVE_RATING_MIN = 7;

	private final MediaRepository mediaRepository;
	private final MediaEmbeddingRepository mediaEmbeddingRepository;
	private final UserMediaRepository userMediaRepository;

	@Transactional(readOnly = true)
	public RecommendationResponseDTO findSimilar(UUID mediaId) {
		if (!mediaRepository.existsById(mediaId)) {
			throw new ResourceNotFoundException("Media not found with id: " + mediaId);
		}
		String embeddingLiteral = mediaEmbeddingRepository.findEmbeddingLiteral(mediaId)
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media is not yet recommendable: " + mediaId));
		List<MediaSimilarityRow> rows = mediaEmbeddingRepository.findSimilar(
				embeddingLiteral, mediaId, SIMILAR_LIMIT);
		return group(rows.stream().map(this::toItem).toList());
	}

	@Transactional(readOnly = true)
	public RecommendationResponseDTO recommendForUser(UUID userId, String typeParam) {
		CatalogType requestedType = parseType(typeParam);
		double globalAverage = globalAverageRating();

		List<UserMedia> positiveItems = userMediaRepository
				.findAllByUserIdAndRatingGreaterThanEqualWithMedia(userId, POSITIVE_RATING_MIN);
		List<RecommendationScoring.WeightedVector> preferenceItems = preferenceVectors(positiveItems);
		if (preferenceItems.isEmpty()) {
			return fallback(userId, requestedType, globalAverage);
		}

		float[] preferenceVector = RecommendationScoring.weightedAverage(preferenceItems);
		String embeddingLiteral = EmbeddingVectorFormat.toLiteral(preferenceVector);
		TasteProfile taste = tasteProfile(positiveItems);

		Map<CatalogType, List<RecommendationItemDTO>> grouped = new EnumMap<>(CatalogType.class);
		for (CatalogType catalogType : CatalogType.values()) {
			int take = catalogType == requestedType ? REQUESTED_TYPE_LIMIT : OTHER_TYPE_LIMIT;
			grouped.put(catalogType, scoreType(
					catalogType,
					embeddingLiteral,
					userId,
					taste,
					globalAverage,
					take));
		}
		return toResponse(grouped);
	}

	private List<RecommendationItemDTO> scoreType(
			CatalogType catalogType,
			String embeddingLiteral,
			UUID userId,
			TasteProfile taste,
			double globalAverage,
			int take) {
		List<MediaSimilarityRow> candidates = mediaEmbeddingRepository.findSimilarByType(
				embeddingLiteral, catalogType.mediaTypeName(), userId, CANDIDATES_PER_TYPE);
		if (candidates.isEmpty()) {
			return List.of();
		}

		Map<UUID, Set<String>> genresByMediaId = loadGenres(candidates);
		boolean hasTypeHistory = taste.typesWithHistory().contains(catalogType.mediaTypeName());
		Set<String> likedGenres = taste.likedGenresByType()
				.getOrDefault(catalogType.mediaTypeName(), Set.of());

		List<ScoredItem> scored = new ArrayList<>(candidates.size());
		for (MediaSimilarityRow row : candidates) {
			double similarity = row.similarity() == null ? 0 : row.similarity();
			double quality = RecommendationScoring.bayesianQuality(
					row.externalRating(), row.externalRatingCount(), globalAverage);
			Double genreAffinity = hasTypeHistory
					? RecommendationScoring.genreAffinity(
							likedGenres, genresByMediaId.getOrDefault(row.mediaId(), Set.of()))
					: null;
			double score = RecommendationScoring.finalScore(similarity, genreAffinity, quality);
			scored.add(new ScoredItem(toItem(row), score));
		}
		scored.sort((left, right) -> {
			int byScore = Double.compare(right.score(), left.score());
			if (byScore != 0) {
				return byScore;
			}
			return left.item().mediaId().compareTo(right.item().mediaId());
		});
		return scored.stream().limit(take).map(ScoredItem::item).toList();
	}

	private RecommendationResponseDTO fallback(UUID userId, CatalogType requestedType, double globalAverage) {
		Map<CatalogType, List<RecommendationItemDTO>> grouped = new EnumMap<>(CatalogType.class);
		for (CatalogType catalogType : CatalogType.values()) {
			int take = catalogType == requestedType ? REQUESTED_TYPE_LIMIT : OTHER_TYPE_LIMIT;
			List<MediaSimilarityRow> rows = mediaEmbeddingRepository.findTopRatedByType(
					catalogType.mediaTypeName(),
					userId,
					globalAverage,
					RecommendationScoring.BAYESIAN_M,
					take);
			grouped.put(catalogType, rows.stream().map(this::toItem).toList());
		}
		return toResponse(grouped);
	}

	private List<RecommendationScoring.WeightedVector> preferenceVectors(List<UserMedia> positiveItems) {
		if (positiveItems.isEmpty()) {
			return List.of();
		}
		List<UUID> mediaIds = positiveItems.stream()
				.map(item -> item.getMedia().getId())
				.toList();
		Map<UUID, float[]> embeddings = mediaEmbeddingRepository.findEmbeddingsByMediaIds(mediaIds);
		List<RecommendationScoring.WeightedVector> vectors = new ArrayList<>();
		for (UserMedia item : positiveItems) {
			float[] embedding = embeddings.get(item.getMedia().getId());
			if (embedding == null || embedding.length != RecommendationScoring.EMBEDDING_DIMENSIONS) {
				continue;
			}
			vectors.add(new RecommendationScoring.WeightedVector(embedding, item.getRating()));
		}
		return vectors;
	}

	private TasteProfile tasteProfile(List<UserMedia> positiveItems) {
		Set<String> typesWithHistory = new HashSet<>();
		Map<String, Set<String>> likedGenresByType = new HashMap<>();
		for (UserMedia item : positiveItems) {
			String typeName = item.getMedia().getMediaType().getName();
			typesWithHistory.add(typeName);
			Set<String> liked = likedGenresByType.computeIfAbsent(typeName, key -> new HashSet<>());
			for (Genre genre : item.getMedia().getGenres()) {
				liked.add(genre.getName());
			}
		}
		return new TasteProfile(typesWithHistory, likedGenresByType);
	}

	private Map<UUID, Set<String>> loadGenres(List<MediaSimilarityRow> candidates) {
		List<UUID> ids = candidates.stream().map(MediaSimilarityRow::mediaId).toList();
		if (ids.isEmpty()) {
			return Map.of();
		}
		Map<UUID, Set<String>> genresByMediaId = new LinkedHashMap<>();
		for (Media media : mediaRepository.findAllWithGenresByIdIn(ids)) {
			Set<String> names = new HashSet<>();
			for (Genre genre : media.getGenres()) {
				names.add(genre.getName());
			}
			genresByMediaId.put(media.getId(), names);
		}
		return genresByMediaId;
	}

	private double globalAverageRating() {
		Double average = mediaRepository.findAverageExternalRating();
		return average == null ? 0 : average;
	}

	private CatalogType parseType(String typeParam) {
		if (typeParam == null || typeParam.isBlank()) {
			throw new BadRequestException("Type is required");
		}
		return CatalogType.fromParam(typeParam)
				.orElseThrow(() -> new BadRequestException(
						"Invalid type. Must be one of: MOVIE, TV, ANIME, GAME"));
	}

	private RecommendationItemDTO toItem(MediaSimilarityRow row) {
		return new RecommendationItemDTO(
				row.mediaId(),
				row.title(),
				row.mediaType(),
				row.releaseYear(),
				row.externalRating(),
				row.externalRatingCount());
	}

	private RecommendationResponseDTO group(List<RecommendationItemDTO> items) {
		Map<CatalogType, List<RecommendationItemDTO>> grouped = new EnumMap<>(CatalogType.class);
		for (CatalogType catalogType : CatalogType.values()) {
			grouped.put(catalogType, new ArrayList<>());
		}
		for (RecommendationItemDTO item : items) {
			for (CatalogType catalogType : CatalogType.values()) {
				if (catalogType.mediaTypeName().equals(item.mediaType())) {
					grouped.get(catalogType).add(item);
					break;
				}
			}
		}
		return toResponse(grouped);
	}

	private RecommendationResponseDTO toResponse(Map<CatalogType, List<RecommendationItemDTO>> grouped) {
		return new RecommendationResponseDTO(
				List.copyOf(grouped.getOrDefault(CatalogType.MOVIE, List.of())),
				List.copyOf(grouped.getOrDefault(CatalogType.TV, List.of())),
				List.copyOf(grouped.getOrDefault(CatalogType.ANIME, List.of())),
				List.copyOf(grouped.getOrDefault(CatalogType.GAME, List.of())));
	}

	private record TasteProfile(Set<String> typesWithHistory, Map<String, Set<String>> likedGenresByType) {
	}

	private record ScoredItem(RecommendationItemDTO item, double score) {
	}
}
