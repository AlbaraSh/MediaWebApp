package com.mediawebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * TMDB client settings bound from {@code tmdb.api.*}.
 * {@code key} must come from config/environment — never hardcoded or logged.
 */
@ConfigurationProperties(prefix = "tmdb.api")
public record TmdbProperties(String key, String baseUrl) {

	@Override
	public String toString() {
		return "TmdbProperties[key=***, baseUrl=" + baseUrl + "]";
	}
}
