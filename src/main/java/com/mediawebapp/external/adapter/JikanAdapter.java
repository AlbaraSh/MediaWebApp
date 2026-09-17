package com.mediawebapp.external.adapter;

import com.mediawebapp.config.JikanProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.jikan.JikanAnimeDetailsResponse;
import com.mediawebapp.external.dto.jikan.JikanSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for Jikan (MyAnimeList). Returns raw Jikan payloads only.
 */
@Component
public class JikanAdapter {

	private static final String PROVIDER_UNAVAILABLE = "Jikan provider is unavailable";

	private final RestClient restClient;

	public JikanAdapter(RestClient.Builder restClientBuilder, JikanProperties properties) {
		this.restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.defaultHeader("User-Agent", "MediaWebApp/0.0.1")
				.build();
	}

	public JikanSearchResponse searchAnime(String query) {
		try {
			JikanSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/anime")
							.queryParam("q", query)
							.queryParam("limit", 10)
							.build())
					.retrieve()
					.onStatus(JikanAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(JikanSearchResponse.class);
			return body != null ? body : new JikanSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public JikanAnimeDetailsResponse getAnime(String malId) {
		try {
			JikanAnimeDetailsResponse body = restClient.get()
					.uri("/anime/{id}", malId)
					.retrieve()
					.onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
						throw new ResourceNotFoundException("Jikan anime not found with id: " + malId);
					})
					.onStatus(JikanAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(JikanAnimeDetailsResponse.class);
			if (body == null || body.data() == null) {
				throw new ResourceNotFoundException("Jikan anime not found with id: " + malId);
			}
			return body;
		} catch (ResourceNotFoundException | ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	private static boolean isRetryableStatus(HttpStatusCode status) {
		return status.value() == 429 || status.is5xxServerError();
	}
}
