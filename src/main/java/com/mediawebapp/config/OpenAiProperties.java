package com.mediawebapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAI client settings bound from {@code openai.api.*}.
 * {@code key} must come from config/environment — never hardcoded or logged.
 */
@ConfigurationProperties(prefix = "openai.api")
public record OpenAiProperties(String key, String baseUrl) {

	@Override
	public String toString() {
		return "OpenAiProperties[key=***, baseUrl=" + baseUrl + "]";
	}
}
