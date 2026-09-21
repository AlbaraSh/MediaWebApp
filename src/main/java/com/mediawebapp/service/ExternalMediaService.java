package com.mediawebapp.service;

import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.dto.ImportMediaResult;
import com.mediawebapp.dto.MediaRequestDTO;
import com.mediawebapp.dto.MediaResponseDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaExternalId;
import com.mediawebapp.entity.MediaType;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.adapter.JikanAdapter;
import com.mediawebapp.external.adapter.RawgAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.mapper.JikanMapper;
import com.mediawebapp.external.mapper.RawgMapper;
import com.mediawebapp.external.mapper.TmdbMapper;
import com.mediawebapp.mapper.MediaMapper;
import com.mediawebapp.repository.MediaExternalIdRepository;
import com.mediawebapp.repository.MediaTypeRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Orchestrates external search, fetch, and import. Adapters are never called
 * from controllers. HTTP calls happen outside a database write transaction.
 */
@Service
@RequiredArgsConstructor
public class ExternalMediaService {

	private static final Set<String> PROVIDERS = Set.of("tmdb", "jikan", "rawg");
	private static final String MOVIE_PREFIX = "movie:";
	private static final String TV_PREFIX = "tv:";

	private final TmdbAdapter tmdbAdapter;
	private final JikanAdapter jikanAdapter;
	private final RawgAdapter rawgAdapter;
	private final TmdbMapper tmdbMapper;
	private final JikanMapper jikanMapper;
	private final RawgMapper rawgMapper;
	private final MediaService mediaService;
	private final MediaTypeRepository mediaTypeRepository;
	private final MediaExternalIdRepository mediaExternalIdRepository;
	private final MediaMapper mediaMapper;
	private final TransactionTemplate transactionTemplate;

	@Cacheable(
			cacheNames = CacheConfig.EXTERNAL_SEARCH,
			key = "T(com.mediawebapp.service.ExternalMediaService).searchCacheKey(#type, #query)",
			condition = "#query != null && !#query.isBlank() && #type != null && !#type.isBlank()")
	public List<ExternalMediaDTO> search(String query, String type) {
		if (query == null || query.isBlank()) {
			throw new BadRequestException("Query is required");
		}
		if (type == null || type.isBlank()) {
			throw new BadRequestException("Type is required");
		}

		String normalizedType = type.toUpperCase(Locale.ROOT);
		List<ExternalMediaDTO> results = switch (normalizedType) {
			case "MOVIE" -> tmdbMapper.toMovieDtos(tmdbAdapter.searchMovies(query));
			case "TV" -> tmdbMapper.toTvDtos(tmdbAdapter.searchTVShows(query));
			case "ANIME" -> jikanMapper.toDtos(jikanAdapter.searchAnime(query));
			case "GAME" -> rawgMapper.toDtos(rawgAdapter.searchGames(query));
			default -> throw new BadRequestException(
					"Invalid type. Must be one of: MOVIE, TV, ANIME, GAME");
		};
		return results.stream().limit(10).toList();
	}

	@Cacheable(
			cacheNames = CacheConfig.EXTERNAL_DETAILS,
			key = "T(com.mediawebapp.service.ExternalMediaService).detailsCacheKey(#provider, #externalId)",
			condition = "#provider != null && !#provider.isBlank() && #externalId != null && !#externalId.isBlank()")
	public ExternalMediaDTO getByExternalId(String provider, String externalId) {
		return fetchExternalDetails(provider, externalId);
	}

	/**
	 * Same fetch+map as {@link #getByExternalId} with no cache. Used by the
	 * rating-refresh job so persisted ratings are never served from a stale details entry.
	 */
	public ExternalMediaDTO getByExternalIdUncached(String provider, String externalId) {
		return fetchExternalDetails(provider, externalId);
	}

	public ImportMediaResult importMedia(ImportMediaRequestDTO request) {
		String lowercaseProvider = requireLowercaseProvider(request.provider());
		String source = lowercaseProvider.toUpperCase(Locale.ROOT);
		String requestExternalId = canonicalizeExternalId(lowercaseProvider, request.externalId());

		var alreadyImported = mediaExternalIdRepository
				.findBySourceAndExternalIdWithMedia(source, requestExternalId);
		if (alreadyImported.isPresent()) {
			return new ImportMediaResult(
					mediaMapper.toResponseDto(alreadyImported.get().getMedia()), false);
		}

		ExternalMediaDTO dto = fetchAndMap(lowercaseProvider, requestExternalId);
		validateRequiredFields(dto);
		String storedExternalId = dto.externalId();

		try {
			return transactionTemplate.execute(status -> completeImport(source, storedExternalId, dto));
		} catch (DataIntegrityViolationException exception) {
			return new ImportMediaResult(loadExisting(source, storedExternalId), false);
		}
	}

	private ImportMediaResult completeImport(String source, String externalId, ExternalMediaDTO dto) {
		var existing = mediaExternalIdRepository.findBySourceAndExternalIdWithMedia(source, externalId);
		if (existing.isPresent()) {
			return new ImportMediaResult(mediaMapper.toResponseDto(existing.get().getMedia()), false);
		}

		MediaType mediaType = mediaTypeRepository.findByName(dto.mediaType())
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media type not found with name: " + dto.mediaType()));

		MediaRequestDTO createRequest = new MediaRequestDTO(
				dto.title(),
				dto.description(),
				dto.releaseYear(),
				mediaType.getId(),
				dto.genres(),
				dto.externalRating(),
				dto.externalRatingCount(),
				dto.posterUrl()
		);
		MediaResponseDTO created = mediaService.createMedia(createRequest);

		Media mediaReference = new Media();
		mediaReference.setId(created.id());

		MediaExternalId mapping = new MediaExternalId();
		mapping.setMedia(mediaReference);
		mapping.setSource(source);
		mapping.setExternalId(externalId);
		mediaExternalIdRepository.saveAndFlush(mapping);

		return new ImportMediaResult(created, true);
	}

	private void validateRequiredFields(ExternalMediaDTO dto) {
		if (dto.releaseYear() == null) {
			throw new BadRequestException(
					"Validation failed",
					Map.of("releaseYear", "Release year is required to import media"));
		}
		if (dto.releaseYear() < 1800 || dto.releaseYear() > 2100) {
			throw new BadRequestException(
					"Validation failed",
					Map.of("releaseYear", "Release year must be between 1800 and 2100"));
		}
		if (dto.title() == null || dto.title().isBlank()) {
			throw new BadRequestException(
					"Validation failed",
					Map.of("title", "Title is required to import media"));
		}
		if (dto.title().length() > 500) {
			throw new BadRequestException(
					"Validation failed",
					Map.of("title", "Title must be at most 500 characters"));
		}
	}

	private MediaResponseDTO loadExisting(String source, String externalId) {
		return transactionTemplate.execute(status -> mediaExternalIdRepository
				.findBySourceAndExternalIdWithMedia(source, externalId)
				.map(mapping -> mediaMapper.toResponseDto(mapping.getMedia()))
				.orElseThrow(() -> new ResourceNotFoundException(
						"Media mapping not found for source " + source + " and id " + externalId)));
	}

	private ExternalMediaDTO fetchExternalDetails(String provider, String externalId) {
		String lowercaseProvider = requireLowercaseProvider(provider);
		if (externalId == null || externalId.isBlank()) {
			throw new BadRequestException("External id is required");
		}
		return fetchAndMap(lowercaseProvider, canonicalizeExternalId(lowercaseProvider, externalId));
	}

	private ExternalMediaDTO fetchAndMap(String lowercaseProvider, String externalId) {
		return switch (lowercaseProvider) {
			case "tmdb" -> fetchTmdb(externalId);
			case "jikan" -> jikanMapper.toDto(jikanAdapter.getAnime(externalId));
			case "rawg" -> rawgMapper.toDto(rawgAdapter.getGame(externalId));
			default -> throw new BadRequestException(
					"Invalid provider. Must be one of: tmdb, jikan, rawg");
		};
	}

	private ExternalMediaDTO fetchTmdb(String externalId) {
		if (externalId.startsWith(MOVIE_PREFIX)) {
			return tmdbMapper.toMovieDto(tmdbAdapter.getMovie(externalId.substring(MOVIE_PREFIX.length())));
		}
		if (externalId.startsWith(TV_PREFIX)) {
			return tmdbMapper.toTvDto(tmdbAdapter.getTVShow(externalId.substring(TV_PREFIX.length())));
		}
		throw new BadRequestException("TMDB external id must start with 'movie:' or 'tv:'");
	}

	private static String canonicalizeExternalId(String lowercaseProvider, String externalId) {
		String trimmed = externalId.trim();
		if (!"tmdb".equals(lowercaseProvider)) {
			return trimmed;
		}
		if (trimmed.startsWith(MOVIE_PREFIX)) {
			String tmdbId = trimmed.substring(MOVIE_PREFIX.length()).trim();
			if (tmdbId.isEmpty()) {
				throw new BadRequestException("TMDB external id must start with 'movie:' or 'tv:'");
			}
			return MOVIE_PREFIX + tmdbId;
		}
		if (trimmed.startsWith(TV_PREFIX)) {
			String tmdbId = trimmed.substring(TV_PREFIX.length()).trim();
			if (tmdbId.isEmpty()) {
				throw new BadRequestException("TMDB external id must start with 'movie:' or 'tv:'");
			}
			return TV_PREFIX + tmdbId;
		}
		throw new BadRequestException("TMDB external id must start with 'movie:' or 'tv:'");
	}

	public static String searchCacheKey(String type, String query) {
		return type.toUpperCase(Locale.ROOT) + ':' + query.trim().toLowerCase(Locale.ROOT);
	}

	public static String detailsCacheKey(String provider, String externalId) {
		String lowercaseProvider = provider.toLowerCase(Locale.ROOT);
		return lowercaseProvider + ':' + canonicalizeExternalId(lowercaseProvider, externalId);
	}

	private static String requireLowercaseProvider(String provider) {
		if (provider == null || provider.isBlank() || !PROVIDERS.contains(provider)) {
			throw new BadRequestException("Invalid provider. Must be one of: tmdb, jikan, rawg");
		}
		return provider;
	}
}
