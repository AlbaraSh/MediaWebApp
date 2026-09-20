package com.mediawebapp.config;

import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The only account allowed to mutate the global catalog via {@code POST/PUT/DELETE /api/media}.
 * Import remains available to any authenticated user.
 */
@ConfigurationProperties(prefix = "app.catalog-admin")
public record CatalogAdminProperties(UUID userId) {
}
