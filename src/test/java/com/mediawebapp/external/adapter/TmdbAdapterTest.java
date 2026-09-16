package com.mediawebapp.external.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;

import com.mediawebapp.config.TmdbProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.tmdb.TmdbMovie;
import com.mediawebapp.external.dto.tmdb.TmdbMovieSearchResponse;
import com.mediawebapp.external.dto.tmdb.TmdbTv;
import com.mediawebapp.external.dto.tmdb.TmdbTvSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class TmdbAdapterTest {

	private MockRestServiceServer server;
	private TmdbAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new TmdbAdapter(builder, new TmdbProperties("test-tmdb-key", "https://api.themoviedb.org/3"));
	}

	@Test
	void searchMovies_callsMovieEndpointAndParsesResults() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/search/movie")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("query", "Fight%20Club"))
				.andExpect(queryParam("api_key", "test-tmdb-key"))
				.andRespond(withSuccess("""
						{
						  "results": [
						    {
						      "id": 550,
						      "title": "Fight Club",
						      "overview": "An insomniac office worker.",
						      "release_date": "1999-10-15"
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		TmdbMovieSearchResponse response = adapter.searchMovies("Fight Club");

		server.verify();
		assertThat(response.results()).hasSize(1);
		TmdbMovie movie = response.results().get(0);
		assertThat(movie.id()).isEqualTo(550);
		assertThat(movie.title()).isEqualTo("Fight Club");
		assertThat(movie.overview()).isEqualTo("An insomniac office worker.");
		assertThat(movie.releaseDate()).isEqualTo("1999-10-15");
	}

	@Test
	void searchTVShows_callsTvEndpointAndParsesResults() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/search/tv")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("query", "Breaking%20Bad"))
				.andExpect(queryParam("api_key", "test-tmdb-key"))
				.andRespond(withSuccess("""
						{
						  "results": [
						    {
						      "id": 1396,
						      "name": "Breaking Bad",
						      "overview": "A chemistry teacher.",
						      "first_air_date": "2008-01-20"
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		TmdbTvSearchResponse response = adapter.searchTVShows("Breaking Bad");

		server.verify();
		assertThat(response.results()).hasSize(1);
		TmdbTv show = response.results().get(0);
		assertThat(show.id()).isEqualTo(1396);
		assertThat(show.name()).isEqualTo("Breaking Bad");
		assertThat(show.firstAirDate()).isEqualTo("2008-01-20");
	}

	@Test
	void searchMovies_doesNotCallTvEndpoint() {
		server.expect(requestTo(containsString("/search/movie")))
				.andRespond(withSuccess("{\"results\":[]}", MediaType.APPLICATION_JSON));

		adapter.searchMovies("matrix");

		server.verify();
	}

	@Test
	void getMovie_parsesDetails() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/movie/550")))
				.andExpect(queryParam("api_key", "test-tmdb-key"))
				.andRespond(withSuccess("""
						{
						  "id": 550,
						  "title": "Fight Club",
						  "overview": "An insomniac office worker.",
						  "release_date": "1999-10-15"
						}
						""", MediaType.APPLICATION_JSON));

		TmdbMovie movie = adapter.getMovie("550");

		server.verify();
		assertThat(movie.title()).isEqualTo("Fight Club");
	}

	@Test
	void getTVShow_parsesDetails() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/tv/1396")))
				.andRespond(withSuccess("""
						{
						  "id": 1396,
						  "name": "Breaking Bad",
						  "overview": "A chemistry teacher.",
						  "first_air_date": "2008-01-20"
						}
						""", MediaType.APPLICATION_JSON));

		TmdbTv show = adapter.getTVShow("1396");

		server.verify();
		assertThat(show.name()).isEqualTo("Breaking Bad");
	}

	@Test
	void getMovie_whenNotFound_throwsResourceNotFound() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/movie/0")))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> adapter.getMovie("0"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("0");
	}

	@Test
	void searchMovies_whenProviderFails_throwsExternalProviderException() {
		server.expect(requestTo(startsWith("https://api.themoviedb.org/3/search/movie")))
				.andRespond(withServerError());

		assertThatThrownBy(() -> adapter.searchMovies("matrix"))
				.isInstanceOf(ExternalProviderException.class)
				.hasMessage("TMDB provider is unavailable");
	}
}
