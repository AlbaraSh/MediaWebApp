package com.mediawebapp.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * In-process Caffeine caches for repeat external reads and catalog get-by-id.
 * TTLs are per cache; provider data is not invalidated by our writes.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	public static final String EXTERNAL_SEARCH = "external-search";
	public static final String EXTERNAL_DETAILS = "external-details";
	public static final String MEDIA_BY_ID = "media-by-id";

	public static final List<String> CACHE_NAMES = List.of(EXTERNAL_SEARCH, EXTERNAL_DETAILS, MEDIA_BY_ID);

	private static final long MAX_SIZE = 1000;

	@Bean
	CacheManager cacheManager() {
		CaffeineCacheManager cacheManager = new CaffeineCacheManager();
		cacheManager.setAllowNullValues(false);
		cacheManager.registerCustomCache(EXTERNAL_SEARCH, caffeine(Duration.ofMinutes(15)).build());
		cacheManager.registerCustomCache(EXTERNAL_DETAILS, caffeine(Duration.ofHours(12)).build());
		cacheManager.registerCustomCache(MEDIA_BY_ID, caffeine(Duration.ofMinutes(30)).build());
		return cacheManager;
	}

	private static Caffeine<Object, Object> caffeine(Duration ttl) {
		return Caffeine.newBuilder()
				.expireAfterWrite(ttl)
				.maximumSize(MAX_SIZE);
	}
}
