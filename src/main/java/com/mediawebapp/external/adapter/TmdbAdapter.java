package com.mediawebapp.external.adapter;

import com.mediawebapp.config.TmdbProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * HTTP adapter for The Movie Database. Returns raw TMDB payloads only.
 * Movie and TV searches use distinct endpoints — never post-filtered.
 */
@Component
public class TmdbAdapter {

	private static final String PROVIDER_UNAVAILABLE = "TMDB provider is unavailable";

	private final RestClient restClient;
	private final String apiKey;

	public TmdbAdapter(RestClient.Builder restClientBuilder, TmdbProperties properties) {
		this.apiKey = properties.key();
		this.restClient = restClientBuilder.clone()
				.baseUrl(properties.baseUrl())
				.build();
	}

	public TmdbMovieSearchResponse searchMovies(String query) {
		try {
			TmdbMovieSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search/movie")
							.queryParam("query", query)
							.queryParam("api_key", apiKey)
							.build())
					.retrieve()
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbMovieSearchResponse.class);
			return body != null ? body : new TmdbMovieSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public TmdbTvSearchResponse searchTVShows(String query) {
		try {
			TmdbTvSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/search/tv")
							.queryParam("query", query)
							.queryParam("api_key", apiKey)
							.build())
					.retrieve()
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbTvSearchResponse.class);
			return body != null ? body : new TmdbTvSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public TmdbMovieSearchResponse listTopRatedMovies(int page) {
		try {
			TmdbMovieSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/movie/top_rated")
							.queryParam("page", page)
							.queryParam("api_key", apiKey)
							.build())
					.retrieve()
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbMovieSearchResponse.class);
			return body != null ? body : new TmdbMovieSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public TmdbTvSearchResponse listTopRatedTv(int page) {
		try {
			TmdbTvSearchResponse body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/tv/top_rated")
							.queryParam("page", page)
							.queryParam("api_key", apiKey)
							.build())
					.retrieve()
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbTvSearchResponse.class);
			return body != null ? body : new TmdbTvSearchResponse(null);
		} catch (ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public TmdbMovie getMovie(String tmdbId) {
		try {
			TmdbMovie body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/movie/{id}")
							.queryParam("api_key", apiKey)
							.build(tmdbId))
					.retrieve()
					.onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
						throw new ResourceNotFoundException("TMDB movie not found with id: " + tmdbId);
					})
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbMovie.class);
			if (body == null) {
				throw new ResourceNotFoundException("TMDB movie not found with id: " + tmdbId);
			}
			return body;
		} catch (ResourceNotFoundException | ExternalProviderException exception) {
			throw exception;
		} catch (RestClientException exception) {
			throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
		}
	}

	public TmdbTv getTVShow(String tmdbId) {
		try {
			TmdbTv body = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.path("/tv/{id}")
							.queryParam("api_key", apiKey)
							.build(tmdbId))
					.retrieve()
					.onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
						throw new ResourceNotFoundException("TMDB TV show not found with id: " + tmdbId);
					})
					.onStatus(TmdbAdapter::isRetryableStatus, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, true);
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new ExternalProviderException(PROVIDER_UNAVAILABLE, false);
					})
					.body(TmdbTv.class);
			if (body == null) {
				throw new ResourceNotFoundException("TMDB TV show not found with id: " + tmdbId);
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
