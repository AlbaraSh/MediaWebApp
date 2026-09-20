package com.mediawebapp.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Browser origins allowed to call the API. Bound from {@code app.cors.allowed-origins}.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
