package com.mediawebapp.schedule;

import com.mediawebapp.config.CacheConfig;
import com.mediawebapp.dto.ExternalMediaDTO;
import com.mediawebapp.entity.Media;
import com.mediawebapp.entity.MediaExternalId;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.repository.MediaExternalIdRepository;
import com.mediawebapp.repository.MediaRepository;
import com.mediawebapp.service.ExternalMediaService;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Daily sequential refresh of stale external ratings. HTTP never runs inside
 * a database transaction. One row's failure does not stop the rest.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "ratings.refresh.enabled", havingValue = "true")
public class RatingRefreshJob {

	static final int MAX_ATTEMPTS = 4;

	private final MediaRepository mediaRepository;
	private final MediaExternalIdRepository mediaExternalIdRepository;
	private final ExternalMediaService externalMediaService;
	private final TransactionTemplate transactionTemplate;
	private final CacheManager cacheManager;
	private final BackoffSleeper backoffSleeper;
	private final int staleMonths;

	public RatingRefreshJob(
			MediaRepository mediaRepository,
			MediaExternalIdRepository mediaExternalIdRepository,
			ExternalMediaService externalMediaService,
			TransactionTemplate transactionTemplate,
			CacheManager cacheManager,
			BackoffSleeper backoffSleeper,
			@Value("${ratings.refresh.stale-months:3}") int staleMonths) {
		this.mediaRepository = mediaRepository;
		this.mediaExternalIdRepository = mediaExternalIdRepository;
		this.externalMediaService = externalMediaService;
		this.transactionTemplate = transactionTemplate;
		this.cacheManager = cacheManager;
		this.backoffSleeper = backoffSleeper;
		this.staleMonths = staleMonths;
	}

	@Scheduled(cron = "${ratings.refresh.cron:0 0 3 * * *}")
	public void refreshStaleRatings() {
		Instant cutoff = Instant.now().atZone(ZoneOffset.UTC).minusMonths(staleMonths).toInstant();
		List<UUID> staleIds = mediaRepository.findIdsWithStaleRatings(cutoff);
		for (UUID mediaId : staleIds) {
			if (Thread.currentThread().isInterrupted()) {
				log.warn("Rating refresh interrupted; stopping remaining rows");
				return;
			}
			try {
				refreshOne(mediaId);
			} catch (RuntimeException exception) {
				log.warn("Unexpected failure refreshing media {}; continuing with remaining rows",
						mediaId, exception);
			}
		}
	}

	void refreshOne(UUID mediaId) {
		MediaExternalId mapping = mediaExternalIdRepository.findFirstByMedia_Id(mediaId).orElse(null);
		if (mapping == null) {
			return;
		}
		String provider = mapping.getSource().toLowerCase(Locale.ROOT);
		String externalId = mapping.getExternalId();

		ExternalMediaDTO dto = fetchWithRetry(mediaId, provider, externalId);
		if (dto == null) {
			return;
		}

		transactionTemplate.execute(status -> {
			Media media = mediaRepository.findById(mediaId).orElse(null);
			if (media == null) {
				return null;
			}
			media.setExternalRating(dto.externalRating());
			media.setExternalRatingCount(dto.externalRatingCount());
			media.setRatingLastUpdatedAt(Instant.now());
			mediaRepository.save(media);
			return null;
		});
		evictMediaById(mediaId);
	}

	private void evictMediaById(UUID mediaId) {
		Cache cache = cacheManager.getCache(CacheConfig.MEDIA_BY_ID);
		if (cache != null) {
			cache.evict(mediaId);
		}
	}

	private ExternalMediaDTO fetchWithRetry(UUID mediaId, String provider, String externalId) {
		for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
			try {
				return externalMediaService.getByExternalIdUncached(provider, externalId);
			} catch (ResourceNotFoundException exception) {
				log.warn("Skipping rating refresh for media {} ({} {}): external id not found",
						mediaId, provider, externalId);
				return null;
			} catch (ExternalProviderException exception) {
				if (!exception.isRetryable()) {
					log.warn("Skipping rating refresh for media {} ({}): non-retryable provider error",
							mediaId, provider);
					return null;
				}
				if (attempt == MAX_ATTEMPTS) {
					log.warn("Giving up rating refresh for media {} ({}) after {} attempts",
							mediaId, provider, MAX_ATTEMPTS);
					return null;
				}
				Duration wait = Duration.ofSeconds(1L << (attempt - 1));
				try {
					backoffSleeper.sleep(wait);
				} catch (InterruptedException interrupted) {
					Thread.currentThread().interrupt();
					log.warn("Rating refresh interrupted while backing off media {}", mediaId);
					return null;
				}
			}
		}
		return null;
	}
}
