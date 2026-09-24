package com.mediawebapp.external.adapter;

import com.mediawebapp.config.RawgProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.rawg.RawgSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for RAWG. Returns raw RAWG payloads only.
 */
@Component
public class RawgAdapter {

	private static final String PROVIDER_UNAVAILABLE = "RAWG provider is unavailable";

	private final RestClient restClient;
	private final String apiKey;

	public RawgAdapter(RestClient.Builder restClientBuilder, RawgProperties properties) {
		this.apiKey = properties.key();
		this.restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.build();
	}

	public RawgSearchResponse searchGames(String query) {
		try {
			RawgSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/games")
							.queryParam("search", query)
							.queryParam("page_size", 10)
							.queryParam("key", apiKey)
							.build())
					.retrieve()
					.onStatus(RawgAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(RawgSearchResponse.class);
			return body != null ? body : new RawgSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public RawgSearchResponse listTopGames(int page) {
		try {
			RawgSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/games")
							.queryParam("ordering", "-metacritic")
							.queryParam("exclude_additions", true)
							.queryParam("page_size", 40)
							.queryParam("page", page)
							.queryParam("key", apiKey)
							.build())
					.retrieve()
					.onStatus(RawgAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(RawgSearchResponse.class);
			return body != null ? body : new RawgSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public RawgGame getGame(String rawgId) {
		try {
			RawgGame body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/games/{id}")
							.queryParam("key", apiKey)
							.build(rawgId))
					.retrieve()
					.onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
						throw new ResourceNotFoundException("RAWG game not found with id: " + rawgId);
					})
					.onStatus(RawgAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(RawgGame.class);
			if (body == null) {
				throw new ResourceNotFoundException("RAWG game not found with id: " + rawgId);
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
