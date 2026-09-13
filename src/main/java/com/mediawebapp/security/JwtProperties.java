package com.mediawebapp.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from {@code jwt.*} in application configuration.
 *
 * @param secret HMAC-SHA256 key; must come from config/env, never hardcoded in code
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret) {
}
