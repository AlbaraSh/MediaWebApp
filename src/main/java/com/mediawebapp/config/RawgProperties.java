package com.mediawebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAWG client settings bound from {@code rawg.api.*}.
 * {@code key} must come from config/environment — never hardcoded or logged.
 */
@ConfigurationProperties(prefix = "rawg.api")
public record RawgProperties(String key, String baseUrl) {

	@Override
	public String toString() {
		return "RawgProperties[key=***, baseUrl=" + baseUrl + "]";
	}
}
