package com.mediawebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jikan client settings bound from {@code jikan.api.*}. No API key is required.
 */
@ConfigurationProperties(prefix = "jikan.api")
public record JikanProperties(String baseUrl) {
}
