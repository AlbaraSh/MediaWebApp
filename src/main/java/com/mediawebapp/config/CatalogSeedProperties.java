package com.mediawebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * One-shot fill of the catalog from provider top lists. Disabled unless
 * {@code catalog.seed.on-startup=true}.
 */
@ConfigurationProperties(prefix = "catalog.seed")
public record CatalogSeedProperties(boolean onStartup, int perType) {

	public CatalogSeedProperties {
		if (perType < 1) {
			perType = 250;
		}
	}
}
