package com.mediawebapp.service;

import com.mediawebapp.config.CatalogSeedProperties;
import com.mediawebapp.dto.ImportMediaRequestDTO;
import com.mediawebapp.exception.BadRequestException;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.adapter.JikanAdapter;
import com.mediawebapp.external.adapter.RawgAdapter;
import com.mediawebapp.external.adapter.TmdbAdapter;
import com.mediawebapp.external.dto.jikan.JikanAnime;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.schedule.BackoffSleeper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Imports the current top titles from TMDB, Jikan, and RAWG. Idempotent:
 * already-imported external ids are skipped by {@link ExternalMediaService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogSeedService {

	private static final Duration TMDB_GAP = Duration.ofMillis(80);
	private static final Duration RAWG_GAP = Duration.ofMillis(80);
	private static final Duration JIKAN_GAP = Duration.ofMillis(800);

	private final CatalogSeedProperties properties;
	private final MediaRepository mediaRepository;
	private final ExternalMediaService externalMediaService;
	private final TmdbAdapter tmdbAdapter;
	private final JikanAdapter jikanAdapter;
	private final RawgAdapter rawgAdapter;
	private final BackoffSleeper backoffSleeper;

	public void seedIfNeeded() {
		int perType = properties.perType();
		long existing = mediaRepository.count();
		long target = (long) perType * 4;
		if (existing >= target) {
			log.info("Catalog seed skipped; {} titles already present (target {})", existing, target);
			return;
		}
		log.info("Catalog seed starting (have {}, targeting {} per type)", existing, perType);
		seedMovies(perType);
		seedTv(perType);
		seedAnime(perType);
		seedGames(perType);
		log.info("Catalog seed finished; catalog now has {} titles", mediaRepository.count());
	}

	private void seedMovies(int perType) {
		List<String> ids = collect(perType, TMDB_GAP, page -> {
			var body = tmdbAdapter.listTopRatedMovies(page);
			if (body.results() == null) {
				return List.of();
			}
			return body.results().stream()
					.map(TmdbMovie::id)
					.filter(id -> id != null && id > 0)
					.map(id -> "movie:" + id)
					.toList();
		});
		importAll("tmdb", ids, TMDB_GAP, "MOVIE");
	}

	private void seedTv(int perType) {
		List<String> ids = collect(perType, TMDB_GAP, page -> {
			var body = tmdbAdapter.listTopRatedTv(page);
			if (body.results() == null) {
				return List.of();
			}
			return body.results().stream()
					.map(TmdbTv::id)
					.filter(id -> id != null && id > 0)
					.map(id -> "tv:" + id)
					.toList();
		});
		importAll("tmdb", ids, TMDB_GAP, "TV");
	}

	private void seedAnime(int perType) {
		List<String> ids = collect(perType, JIKAN_GAP, page -> {
			var body = jikanAdapter.listTopAnime(page);
			if (body.data() == null) {
				return List.of();
			}
			return body.data().stream()
					.map(JikanAnime::malId)
					.filter(id -> id != null && id > 0)
					.map(String::valueOf)
					.toList();
		});
		importAll("jikan", ids, JIKAN_GAP, "ANIME");
	}

	private void seedGames(int perType) {
		List<String> ids = collect(perType, RAWG_GAP, page -> {
			var body = rawgAdapter.listTopGames(page);
			if (body.results() == null) {
				return List.of();
			}
			return body.results().stream()
					.map(RawgGame::id)
					.filter(id -> id != null && id > 0)
					.map(String::valueOf)
					.toList();
		});
		importAll("rawg", ids, RAWG_GAP, "GAME");
	}

	private List<String> collect(int perType, Duration gap, IntFunction<List<String>> pageLoader) {
		List<String> ids = new ArrayList<>();
		int page = 1;
		while (ids.size() < perType) {
			List<String> batch;
			try {
				batch = pageLoader.apply(page);
			} catch (ExternalProviderException exception) {
				log.warn("Catalog seed list page {} failed: {}", page, exception.getMessage());
				break;
			}
			if (batch == null || batch.isEmpty()) {
				break;
			}
			for (String id : batch) {
				if (ids.size() >= perType) {
					break;
				}
				if (!ids.contains(id)) {
					ids.add(id);
				}
			}
			page++;
			pause(gap);
		}
		return ids;
	}

	private void importAll(String provider, List<String> externalIds, Duration gap, String typeLabel) {
		int created = 0;
		int skipped = 0;
		int failed = 0;
		for (String externalId : externalIds) {
			try {
				var result = externalMediaService.importMedia(new ImportMediaRequestDTO(provider, externalId));
				if (result.created()) {
					created++;
				} else {
					skipped++;
				}
			} catch (BadRequestException | ResourceNotFoundException | ExternalProviderException exception) {
				failed++;
				log.warn("Catalog seed skipped {} {}: {}", typeLabel, externalId, exception.getMessage());
			} catch (RuntimeException exception) {
				failed++;
				log.warn("Catalog seed failed {} {}: {}", typeLabel, externalId, exception.getMessage());
			}
			pause(gap);
		}
		log.info("Catalog seed {}: created={}, skipped={}, failed={}", typeLabel, created, skipped, failed);
	}

	private void pause(Duration gap) {
		try {
			backoffSleeper.sleep(gap);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}
}
